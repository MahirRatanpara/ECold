// Template Validation Test Helper
// Copy this into browser console on the templates page to test TC-TEMP-004

const testCases = [
  {
    name: "Valid Template",
    data: {
      name: "Test Valid Template",
      subject: "Application for {Role} at {Company}",
      body: "Dear {RecruiterName}, I am {MyName} and interested in the position.",
      category: "OUTREACH",
      status: "DRAFT"
    },
    shouldPass: true
  },
  {
    name: "Invalid Placeholder Names",
    data: {
      name: "Test Invalid Names",
      subject: "Application for {Position} at {CompanyName}",
      body: "Dear {Recruiter}, I am interested.",
      category: "OUTREACH", 
      status: "DRAFT"
    },
    shouldPass: false,
    expectedErrors: ["Position", "CompanyName", "Recruiter"]
  },
  {
    name: "Unmatched Braces",
    data: {
      name: "Test Unmatched Braces",
      subject: "Application for {Role at {Company}",
      body: "Dear {RecruiterName}, I am interested.",
      category: "OUTREACH",
      status: "DRAFT"
    },
    shouldPass: false,
    expectedErrors: ["unmatched braces"]
  },
  {
    name: "Empty Placeholders",
    data: {
      name: "Test Empty Placeholders",
      subject: "Application for {} at {Company}",
      body: "Dear {RecruiterName}, I am interested.",
      category: "OUTREACH",
      status: "DRAFT"
    },
    shouldPass: false,
    expectedErrors: ["empty", "{}"]
  }
];

async function runTemplateValidationTests() {
  console.log("🧪 Running Template Validation Tests (TC-TEMP-004)");
  console.log("=".repeat(60));
  
  let passed = 0;
  let failed = 0;
  
  for (const testCase of testCases) {
    console.log(`\n--- Testing: ${testCase.name} ---`);
    
    try {
      const response = await fetch('/email-templates', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('authToken') || sessionStorage.getItem('authToken')}`
        },
        body: JSON.stringify(testCase.data)
      });
      
      const result = await response.json();
      
      if (testCase.shouldPass) {
        if (response.ok) {
          console.log("✅ PASS: Template validation passed as expected");
          passed++;
          // Clean up - delete the test template
          if (result.id) {
            await fetch(`/email-templates/${result.id}`, {
              method: 'DELETE',
              headers: {
                'Authorization': `Bearer ${localStorage.getItem('authToken') || sessionStorage.getItem('authToken')}`
              }
            });
          }
        } else {
          console.log("❌ FAIL: Valid template was rejected");
          console.log("Error:", result);
          failed++;
        }
      } else {
        if (!response.ok) {
          console.log("✅ PASS: Invalid template was rejected as expected");
          console.log("Validation error:", result.message || result.error);
          
          // Check if expected errors are in the message
          const errorMessage = (result.message || result.error || '').toLowerCase();
          const hasExpectedErrors = testCase.expectedErrors.some(expected => 
            errorMessage.includes(expected.toLowerCase())
          );
          
          if (hasExpectedErrors) {
            console.log("✅ Error message contains expected validation errors");
          } else {
            console.log("⚠️ Error message doesn't contain expected errors:", testCase.expectedErrors);
          }
          
          passed++;
        } else {
          console.log("❌ FAIL: Invalid template was accepted");
          console.log("Response:", result);
          failed++;
        }
      }
      
    } catch (error) {
      console.log("❌ FAIL: Network error");
      console.error(error);
      failed++;
    }
    
    // Small delay between tests
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  
  console.log("\n" + "=".repeat(60));
  console.log(`📊 Test Results: ${passed} passed, ${failed} failed`);
  
  if (failed === 0) {
    console.log("🎉 ALL TEMPLATE VALIDATION TESTS PASSED!");
    console.log("TC-TEMP-004: Template Placeholder Validation ✅");
  } else {
    console.log("⚠️ Some tests failed. Check template validation implementation.");
  }
}

// Run the tests
runTemplateValidationTests();
