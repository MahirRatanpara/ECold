package com.ecold.service.impl;

import com.ecold.entity.IncomingEmail;
import com.ecold.entity.User;
import com.ecold.repository.IncomingEmailRepository;
import com.ecold.repository.UserRepository;
import com.ecold.service.IncomingEmailService;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomingEmailServiceImpl implements IncomingEmailService {
    
    private final IncomingEmailRepository incomingEmailRepository;
    private final UserRepository userRepository;
    
    private static final Set<String> JOB_KEYWORDS = Set.of(
        "application", "shortlisted", "interview", "resume", "recruiter", "hr", "position", 
        "job", "opportunity", "candidate", "hiring", "selected", "rejected", "offer",
        "screening", "assessment", "placement", "career", "employment"
    );
    
    private static final Set<String> TRUSTED_DOMAINS = Set.of(
        "naukri.com", "linkedin.com", "indeed.com", "monster.com", "glassdoor.com",
        "shine.com", "timesjobs.com", "foundit.in", "instahyre.com"
    );
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "(?i)(application|shortlist|interview|resume|recruiter|hr|position|job|opportunity|candidate|hiring)"
    );
    
    @Override
    @Transactional
    public void scanIncomingEmails(User user) {
        try {
            Gmail gmail = createGmailService(user);
            
            String query = "is:unread newer_than:7d";
            ListMessagesResponse response = gmail.users().messages().list("me").setQ(query).execute();
            
            if (response.getMessages() != null) {
                for (com.google.api.services.gmail.model.Message messageRef : response.getMessages()) {
                    Message message = gmail.users().messages().get("me", messageRef.getId()).execute();
                    
                    if (!incomingEmailRepository.existsByUserAndMessageId(user, message.getId())) {
                        processGmailMessage(user, message);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan incoming emails for user {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }
    
    @Override
    public IncomingEmail.EmailCategory categorizeEmail(String subject, String body, String senderEmail) {
        String content = (subject + " " + body).toLowerCase();
        String senderDomain = extractDomain(senderEmail);
        
        if (TRUSTED_DOMAINS.contains(senderDomain)) {
            if (content.contains("shortlist") || content.contains("interview") || content.contains("selected")) {
                return IncomingEmail.EmailCategory.SHORTLIST_INTERVIEW;
            }
            if (content.contains("application") && content.contains("update")) {
                return IncomingEmail.EmailCategory.APPLICATION_UPDATE;
            }
            if (content.contains("reject") || content.contains("closed") || content.contains("unsuccessful")) {
                return IncomingEmail.EmailCategory.REJECTION_CLOSED;
            }
            return IncomingEmail.EmailCategory.RECRUITER_OUTREACH;
        }
        
        boolean hasJobKeywords = JOB_KEYWORDS.stream()
            .anyMatch(keyword -> content.contains(keyword));
        
        if (hasJobKeywords) {
            if (content.contains("shortlist") || content.contains("interview")) {
                return IncomingEmail.EmailCategory.SHORTLIST_INTERVIEW;
            }
            if (content.contains("reject") || content.contains("regret")) {
                return IncomingEmail.EmailCategory.REJECTION_CLOSED;
            }
            if (content.contains("application") || content.contains("opportunity")) {
                return IncomingEmail.EmailCategory.APPLICATION_UPDATE;
            }
            return IncomingEmail.EmailCategory.RECRUITER_OUTREACH;
        }
        
        if (isSpamIndicator(subject, body, senderEmail)) {
            return IncomingEmail.EmailCategory.SPAM;
        }
        
        return IncomingEmail.EmailCategory.UNKNOWN;
    }
    
    @Override
    public Page<IncomingEmail> getIncomingEmails(User user, int page, int size) {
        return incomingEmailRepository.findByUser(user, PageRequest.of(page, size));
    }
    
    @Override
    public Page<IncomingEmail> getIncomingEmailsByCategory(User user, IncomingEmail.EmailCategory category, int page, int size) {
        return incomingEmailRepository.findByUserAndCategory(user, category, PageRequest.of(page, size));
    }
    
    @Override
    public List<IncomingEmail> getUnreadEmails(User user) {
        return incomingEmailRepository.findByUserAndIsReadFalse(user);
    }
    
    @Override
    @Transactional
    public void markAsRead(Long emailId) {
        IncomingEmail email = incomingEmailRepository.findById(emailId)
            .orElseThrow(() -> new RuntimeException("Email not found"));
        email.setRead(true);
        incomingEmailRepository.save(email);
    }
    
    @Override
    public Long getUnreadCount(User user, IncomingEmail.EmailCategory category) {
        return incomingEmailRepository.countUnreadByUserAndCategory(user, category);
    }
    
    @Override
    @Transactional
    public void processIncomingEmails() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getProvider() == User.Provider.GOOGLE && user.getAccessToken() != null) {
                scanIncomingEmails(user);
            }
        }
    }

    @Override
    @Transactional
    public void refreshUserEmails(User user) {
        if (user.getProvider() == User.Provider.GOOGLE && user.getAccessToken() != null) {
            log.info("Refreshing emails for user: {}", user.getEmail());
            scanIncomingEmails(user);
        } else {
            log.warn("Cannot refresh emails for user {} - missing Google OAuth credentials", user.getEmail());
            throw new RuntimeException("User does not have valid Google OAuth credentials for email access");
        }
    }
    
    private Gmail createGmailService(User user) throws Exception {
        GoogleCredential credential = new GoogleCredential.Builder()
            .setTransport(new NetHttpTransport())
            .setJsonFactory(GsonFactory.getDefaultInstance())
            .build()
            .setAccessToken(user.getAccessToken())
            .setRefreshToken(user.getRefreshToken());
        
        return new Gmail.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("ECold")
            .build();
    }
    
    private void processGmailMessage(User user, Message message) {
        try {
            IncomingEmail incomingEmail = new IncomingEmail();
            incomingEmail.setUser(user);
            incomingEmail.setMessageId(message.getId());
            incomingEmail.setThreadId(message.getThreadId());
            
            MessagePart payload = message.getPayload();
            if (payload != null && payload.getHeaders() != null) {
                for (MessagePartHeader header : payload.getHeaders()) {
                    switch (header.getName().toLowerCase()) {
                        case "from":
                            String from = header.getValue();
                            incomingEmail.setSenderEmail(extractEmail(from));
                            incomingEmail.setSenderName(extractName(from));
                            break;
                        case "subject":
                            incomingEmail.setSubject(header.getValue());
                            break;
                        case "date":
                            // Parse date if needed
                            break;
                    }
                }
            }
            
            String body = extractMessageBody(payload);
            incomingEmail.setBody(body);
            
            IncomingEmail.EmailCategory category = categorizeEmail(
                incomingEmail.getSubject(), 
                body, 
                incomingEmail.getSenderEmail()
            );
            incomingEmail.setCategory(category);
            
            if (category == IncomingEmail.EmailCategory.SHORTLIST_INTERVIEW) {
                incomingEmail.setPriority(IncomingEmail.EmailPriority.HIGH);
            } else if (category == IncomingEmail.EmailCategory.RECRUITER_OUTREACH) {
                incomingEmail.setPriority(IncomingEmail.EmailPriority.NORMAL);
            } else {
                incomingEmail.setPriority(IncomingEmail.EmailPriority.LOW);
            }
            
            incomingEmail.setReceivedAt(LocalDateTime.now());
            incomingEmail.setProcessed(true);
            
            incomingEmailRepository.save(incomingEmail);
            
        } catch (Exception e) {
            log.error("Failed to process Gmail message {}: {}", message.getId(), e.getMessage(), e);
        }
    }
    
    private String extractMessageBody(MessagePart part) {
        if (part == null) return "";
        
        if (part.getMimeType().equals("text/plain") && part.getBody() != null && part.getBody().getData() != null) {
            return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
        }
        
        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                String body = extractMessageBody(subPart);
                if (!body.isEmpty()) {
                    return body;
                }
            }
        }
        
        return "";
    }
    
    private String extractEmail(String from) {
        int start = from.indexOf('<');
        int end = from.indexOf('>');
        if (start != -1 && end != -1 && end > start) {
            return from.substring(start + 1, end);
        }
        return from;
    }
    
    private String extractName(String from) {
        int start = from.indexOf('<');
        if (start != -1) {
            return from.substring(0, start).trim().replaceAll("\"", "");
        }
        return "";
    }
    
    private String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        return atIndex != -1 ? email.substring(atIndex + 1).toLowerCase() : "";
    }
    
    private boolean isSpamIndicator(String subject, String body, String senderEmail) {
        String content = (subject + " " + body).toLowerCase();
        String[] spamKeywords = {"lottery", "winner", "congratulations", "claim now", "urgent", "act now"};
        return Arrays.stream(spamKeywords).anyMatch(content::contains);
    }
}