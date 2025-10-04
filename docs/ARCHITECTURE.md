# ECold - Technical Architecture & Implementation Guide

## 🏗️ System Architecture

### High-Level Architecture
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Angular SPA   │────│   Spring Boot    │────│   PostgreSQL    │
│  (Frontend UI)  │    │   (Backend API)  │    │   (Database)    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌────────┴────────┐             │
         │              │                 │             │
         └──────────────│   OAuth 2.0     │─────────────┘
                        │ (Google/MS)     │
                        └─────────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
            ┌───────▼────────┐      ┌────────▼────────┐
            │  Gmail/Outlook │      │    Redis Cache  │
            │   (Email API)  │      │   (Sessions)    │
            └────────────────┘      └─────────────────┘
```

### Technology Stack

#### Backend Stack
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17 (LTS)
- **Security**: Spring Security + OAuth 2.0
- **Database**: PostgreSQL (Primary), H2 (Development)
- **Caching**: Redis (Production), In-Memory (Development)
- **Email Integration**: Gmail API, Microsoft Graph API
- **Job Scheduling**: Quartz Scheduler
- **Batch Processing**: Spring Batch
- **Documentation**: OpenAPI/Swagger
- **Testing**: JUnit 5, Mockito, TestContainers

#### Frontend Stack
- **Framework**: Angular 17
- **Language**: TypeScript 5.2
- **UI Components**: Angular Material 17
- **Charts**: Chart.js + ng2-charts
- **State Management**: RxJS + Services
- **HTTP Client**: Angular HttpClient
- **Authentication**: JWT + OAuth2
- **Styling**: SCSS + Angular Flex Layout
- **Testing**: Jasmine + Karma

#### DevOps & Infrastructure
- **Containerization**: Docker + Docker Compose
- **Build Tools**: Maven (Backend), Angular CLI (Frontend)
- **CI/CD**: GitHub Actions / Jenkins
- **Monitoring**: Spring Boot Actuator + Micrometer
- **Logging**: SLF4J + Logback
- **Documentation**: Markdown + GitHub Pages

## 📊 Database Schema

### Core Entities

#### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    profile_picture TEXT,
    provider VARCHAR(50) NOT NULL, -- GOOGLE, MICROSOFT, LOCAL
    provider_id VARCHAR(255),
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider ON users(provider, provider_id);
```

#### Recruiter Contacts Table
```sql
CREATE TABLE recruiter_contacts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    recruiter_name VARCHAR(255),
    company_name VARCHAR(255) NOT NULL,
    job_role VARCHAR(255) NOT NULL,
    linkedin_profile TEXT,
    notes TEXT,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, CONTACTED, RESPONDED, etc.
    last_contacted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_recruiters_user_id ON recruiter_contacts(user_id);
CREATE INDEX idx_recruiters_email ON recruiter_contacts(user_id, email);
CREATE INDEX idx_recruiters_status ON recruiter_contacts(status);
```

#### Email Campaigns Table
```sql
CREATE TABLE email_campaigns (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    template_id BIGINT REFERENCES email_templates(id),
    resume_id BIGINT REFERENCES resumes(id),
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, SCHEDULED, RUNNING, etc.
    schedule_type VARCHAR(50) DEFAULT 'ONE_TIME',
    scheduled_at TIMESTAMP,
    batch_size INTEGER DEFAULT 5,
    daily_limit INTEGER DEFAULT 50,
    cc_emails TEXT,
    bcc_emails TEXT,
    total_recipients INTEGER DEFAULT 0,
    sent_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Email Logs Table
```sql
CREATE TABLE email_logs (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT REFERENCES email_campaigns(id),
    recruiter_contact_id BIGINT REFERENCES recruiter_contacts(id),
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT,
    status VARCHAR(50) NOT NULL, -- PENDING, SENT, DELIVERED, OPENED, etc.
    error_message TEXT,
    message_id VARCHAR(255),
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    opened_at TIMESTAMP,
    clicked_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_logs_campaign ON email_logs(campaign_id);
CREATE INDEX idx_email_logs_status ON email_logs(status);
CREATE INDEX idx_email_logs_message_id ON email_logs(message_id);
```

#### Incoming Emails Table
```sql
CREATE TABLE incoming_emails (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_id VARCHAR(255) NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    sender_name VARCHAR(255),
    subject VARCHAR(500) NOT NULL,
    body TEXT,
    html_body TEXT,
    category VARCHAR(50), -- APPLICATION_UPDATE, SHORTLIST_INTERVIEW, etc.
    priority VARCHAR(20) DEFAULT 'NORMAL',
    is_read BOOLEAN DEFAULT FALSE,
    is_processed BOOLEAN DEFAULT FALSE,
    received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    thread_id VARCHAR(255),
    keywords TEXT,
    confidence_score DECIMAL(3,2)
);

CREATE INDEX idx_incoming_emails_user_id ON incoming_emails(user_id);
CREATE INDEX idx_incoming_emails_category ON incoming_emails(category);
CREATE INDEX idx_incoming_emails_is_read ON incoming_emails(is_read);
CREATE UNIQUE INDEX idx_incoming_emails_unique ON incoming_emails(user_id, message_id);
```

## 🔐 Security Implementation

### Authentication & Authorization

#### OAuth 2.0 Flow
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo -> 
                    userInfo.userService(customOAuth2UserService))
                .successHandler(authenticationSuccessHandler))
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }
}
```

#### JWT Token Management
```java
@Service
public class JwtTokenService {
    
    private final String secret = "${app.jwt.secret}";
    private final long expiration = ${app.jwt.expiration};
    
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
}
```

### Data Protection

#### Sensitive Data Encryption
```java
@Component
public class TokenEncryption {
    
    @Value("${app.encryption.key}")
    private String encryptionKey;
    
    public String encryptToken(String token) {
        // AES-256 encryption for stored tokens
        return AESUtil.encrypt(token, encryptionKey);
    }
    
    public String decryptToken(String encryptedToken) {
        return AESUtil.decrypt(encryptedToken, encryptionKey);
    }
}
```

#### Rate Limiting
```java
@Component
public class RateLimitingService {
    
    @Cacheable(value = "rate_limit", key = "#userId")
    public boolean checkRateLimit(Long userId, int maxRequests) {
        // Redis-based rate limiting implementation
        String key = "rate_limit:" + userId;
        String count = redisTemplate.opsForValue().get(key);
        
        if (count == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofHours(1));
            return true;
        }
        
        return Integer.parseInt(count) < maxRequests;
    }
}
```

## 📧 Email Integration Architecture

### Gmail API Integration
```java
@Service
public class GmailService implements EmailService {
    
    private Gmail createGmailService(User user) throws IOException {
        GoogleCredential credential = new GoogleCredential.Builder()
            .setTransport(new NetHttpTransport())
            .setJsonFactory(JacksonFactory.getDefaultInstance())
            .build()
            .setAccessToken(user.getAccessToken())
            .setRefreshToken(user.getRefreshToken());
        
        return new Gmail.Builder(new NetHttpTransport(), 
            JacksonFactory.getDefaultInstance(), credential)
            .setApplicationName("ECold")
            .build();
    }
    
    @Async
    @Retryable(value = {Exception.class}, maxAttempts = 3)
    public CompletableFuture<EmailLog> sendEmailAsync(EmailRequest request) {
        // Asynchronous email sending with retry logic
    }
}
```

### Email Categorization Algorithm
```java
@Component
public class EmailCategorizer {
    
    private static final Map<String, Set<String>> CATEGORY_KEYWORDS = Map.of(
        "SHORTLIST_INTERVIEW", Set.of("shortlist", "interview", "selected"),
        "APPLICATION_UPDATE", Set.of("application", "update", "status"),
        "REJECTION_CLOSED", Set.of("reject", "regret", "closed", "unsuccessful"),
        "RECRUITER_OUTREACH", Set.of("opportunity", "position", "role", "hiring")
    );
    
    public EmailCategory categorizeEmail(String subject, String body, String senderEmail) {
        String content = (subject + " " + body).toLowerCase();
        String domain = extractDomain(senderEmail);
        
        // Trust score based on sender domain
        double trustScore = calculateTrustScore(domain);
        
        // Keyword matching with weighted scoring
        Map<EmailCategory, Double> scores = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            double score = calculateKeywordScore(content, entry.getValue()) * trustScore;
            if (score > 0.3) {
                scores.put(EmailCategory.valueOf(entry.getKey()), score);
            }
        }
        
        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(EmailCategory.UNKNOWN);
    }
}
```

## 📊 Batch Processing & Scheduling

### Email Campaign Processing
```java
@Component
@StepScope
public class EmailCampaignProcessor implements ItemProcessor<RecruiterContact, EmailLog> {
    
    @Override
    public EmailLog process(RecruiterContact recruiter) throws Exception {
        // Rate limiting check
        if (!rateLimitingService.checkRateLimit(recruiter.getUser().getId(), 100)) {
            throw new RateLimitExceededException("Daily email limit reached");
        }
        
        // Template processing with placeholders
        String processedSubject = templateEngine.process(template.getSubject(), 
            createTemplateContext(recruiter));
        String processedBody = templateEngine.process(template.getBody(), 
            createTemplateContext(recruiter));
        
        return emailService.sendEmail(recruiter, processedSubject, processedBody);
    }
}
```

### Scheduled Tasks
```java
@Component
public class ScheduledTasks {
    
    @Scheduled(fixedRate = 3600000) // Every hour
    public void scanIncomingEmails() {
        List<User> users = userService.findActiveUsers();
        users.parallelStream()
            .filter(user -> user.getProvider() == Provider.GOOGLE)
            .forEach(incomingEmailService::scanIncomingEmails);
    }
    
    @Scheduled(cron = "0 0 9 * * MON-FRI") // Weekdays at 9 AM
    public void processScheduledCampaigns() {
        List<EmailCampaign> campaigns = campaignService.findScheduledCampaigns();
        campaigns.forEach(campaignService::executeCampaign);
    }
}
```

## 🚀 Performance Optimizations

### Caching Strategy
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());
        
        return builder.build();
    }
    
    private RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```

### Database Optimizations
```java
@Repository
public interface RecruiterContactRepository extends JpaRepository<RecruiterContact, Long> {
    
    @Query("SELECT rc FROM RecruiterContact rc " +
           "JOIN FETCH rc.user " +
           "WHERE rc.user.id = :userId AND rc.status = :status")
    List<RecruiterContact> findByUserIdAndStatusWithUser(
        @Param("userId") Long userId, 
        @Param("status") ContactStatus status);
    
    @Modifying
    @Query("UPDATE RecruiterContact rc SET rc.lastContactedAt = :now " +
           "WHERE rc.id IN :ids")
    void updateLastContactedBatch(@Param("ids") List<Long> ids, 
                                 @Param("now") LocalDateTime now);
}
```

## 🔍 Monitoring & Observability

### Application Metrics
```java
@Component
public class CustomMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter emailsSentCounter;
    private final Timer emailProcessingTimer;
    
    public CustomMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.emailsSentCounter = Counter.builder("emails.sent.total")
            .description("Total number of emails sent")
            .register(meterRegistry);
        this.emailProcessingTimer = Timer.builder("email.processing.time")
            .description("Email processing time")
            .register(meterRegistry);
    }
    
    public void incrementEmailsSent(String status, String provider) {
        emailsSentCounter.increment(
            Tags.of("status", status, "provider", provider));
    }
}
```

### Health Checks
```java
@Component
public class EmailServiceHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check Gmail API connectivity
            boolean gmailHealthy = gmailService.checkConnectivity();
            
            // Check database connectivity
            boolean dbHealthy = jdbcTemplate.queryForObject(
                "SELECT 1", Integer.class) == 1;
            
            if (gmailHealthy && dbHealthy) {
                return Health.up()
                    .withDetail("gmail", "Connected")
                    .withDetail("database", "Connected")
                    .build();
            } else {
                return Health.down()
                    .withDetail("gmail", gmailHealthy ? "Connected" : "Failed")
                    .withDetail("database", dbHealthy ? "Connected" : "Failed")
                    .build();
            }
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

## 🐳 Containerization

### Backend Dockerfile
```dockerfile
FROM openjdk:17-jdk-alpine

VOLUME /tmp

COPY target/ecold-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
```

### Frontend Dockerfile
```dockerfile
FROM node:18-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build --prod

FROM nginx:alpine
COPY --from=build /app/dist/ecold-frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### Docker Compose for Development
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:13
    environment:
      POSTGRES_DB: ecold
      POSTGRES_USER: ecold_user
      POSTGRES_PASSWORD: ecold_pass
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:6-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data

  backend:
    build:
      context: ./backend
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/ecold
      DATABASE_USERNAME: ecold_user
      DATABASE_PASSWORD: ecold_pass
      REDIS_HOST: redis
      REDIS_PORT: 6379
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
    volumes:
      - ./uploads:/app/uploads

  frontend:
    build:
      context: ./frontend
    ports:
      - "4200:80"
    depends_on:
      - backend

volumes:
  postgres_data:
  redis_data:
```

## 🧪 Testing Strategy

### Backend Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class EmailServiceIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @MockBean
    private GmailService gmailService;
    
    @Test
    void sendEmailTest() {
        // Given
        EmailRequest request = EmailRequest.builder()
            .recipientEmail("test@example.com")
            .subject("Test Subject")
            .body("Test Body")
            .build();
        
        EmailLog expectedResult = new EmailLog();
        expectedResult.setStatus(EmailLog.EmailStatus.SENT);
        
        when(gmailService.sendEmail(any(), any(), any(), any(), any()))
            .thenReturn(expectedResult);
        
        // When
        ResponseEntity<EmailLog> response = restTemplate.postForEntity(
            "/api/emails/send", request, EmailLog.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(EmailLog.EmailStatus.SENT);
    }
}
```

### Frontend Testing
```typescript
describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let dashboardService: jasmine.SpyObj<DashboardService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('DashboardService', ['getDashboardStats']);

    await TestBed.configureTestingModule({
      declarations: [DashboardComponent],
      providers: [
        { provide: DashboardService, useValue: spy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    dashboardService = TestBed.inject(DashboardService) as jasmine.SpyObj<DashboardService>;
  });

  it('should load dashboard stats on init', () => {
    const mockStats = { totalRecruiters: 100, emailsSent: 50 };
    dashboardService.getDashboardStats.and.returnValue(of(mockStats));

    component.ngOnInit();

    expect(dashboardService.getDashboardStats).toHaveBeenCalled();
    expect(component.dashboardStats).toEqual(mockStats);
  });
});
```

## 📈 Scalability Considerations

### Horizontal Scaling
- **Load Balancing**: Multiple backend instances behind nginx/HAProxy
- **Database Sharding**: Partition by user_id for large-scale deployments
- **Microservices**: Split into email-service, user-service, analytics-service
- **Message Queues**: RabbitMQ/Apache Kafka for asynchronous processing

### Performance Monitoring
- **APM Tools**: New Relic, Datadog, or Application Insights
- **Log Aggregation**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Metrics Collection**: Prometheus + Grafana
- **Error Tracking**: Sentry for real-time error monitoring

### Future Architecture Evolution
```
Phase 1: Monolithic (Current)
├── Spring Boot Application
├── Angular Frontend
└── PostgreSQL Database

Phase 2: Service-Oriented
├── API Gateway (Spring Cloud Gateway)
├── User Service
├── Email Service
├── Analytics Service
└── Shared Database

Phase 3: Microservices + Event-Driven
├── API Gateway
├── Individual Services with Own Databases
├── Event Bus (Kafka/RabbitMQ)
├── CQRS + Event Sourcing
└── Distributed Caching (Redis Cluster)
```

This technical architecture provides a solid foundation for building a scalable, maintainable, and secure email automation platform while maintaining flexibility for future enhancements and scaling requirements.