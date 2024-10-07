package com.interv.java_codequality_api;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import net.sourceforge.pmd.*;
import net.sourceforge.pmd.lang.LanguageRegistry;


import java.io.*;
import java.util.*;

@SpringBootApplication
public class CodeQualityApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeQualityApplication.class, args);
    }
}

// Request DTO
class CodeAnalysisRequest {
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

// Response DTO
class CodeAnalysisResponse {
    private List<Map<String, Object>> results;
    private String message;

    public CodeAnalysisResponse(List<Map<String, Object>> results, String message) {
        this.results = results;
        this.message = message;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public String getMessage() {
        return message;
    }
}

@RestController
@RequestMapping("/api")
class CodeAnalysisController {
    
    private final CodeAnalyzer analyzer;
    
    public CodeAnalysisController() {
        this.analyzer = new CodeAnalyzer();
    }
    
    @PostMapping("/analyze")
    public ResponseEntity<CodeAnalysisResponse> analyzeCode(@RequestBody CodeAnalysisRequest request) {
        try {
            List<Map<String, Object>> results = analyzer.analyze(request.getCode());
            List<Map<String, Object>> filteredResults = new ArrayList<>();
            for (Map<String, Object> result : results) {
                Map<String, Object> filteredResult = new HashMap<>();
                filteredResult.put("line", result.get("line"));
                filteredResult.put("description", result.get("description"));
                filteredResults.add(filteredResult);
            }
            CodeAnalysisResponse response = new CodeAnalysisResponse(filteredResults, "Code analyzed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            List<Map<String, Object>> errorResult = new ArrayList<>();
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Error during analysis: " + e.getMessage());
            errorResult.add(error);
            CodeAnalysisResponse errorResponse = new CodeAnalysisResponse(errorResult, "Error analyzing code");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

class CodeAnalyzer {
    private final PMDConfiguration configuration;
    private final RuleSetFactory ruleSetFactory;
    
    @SuppressWarnings("deprecation")
    public CodeAnalyzer() {
        configuration = new PMDConfiguration();
        configuration.setMinimumPriority(RulePriority.MEDIUM);
        configuration.setDefaultLanguageVersion(
            LanguageRegistry.findLanguageByTerseName("java").getVersion("17")
        );
        
        ruleSetFactory = new RuleSetFactory();
    }
    
    public List<Map<String, Object>> analyze(String code) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            File tempFile = createTempFile(code);
            
            RuleContext ctx = new RuleContext();
            Report report = new Report();
            ctx.setReport(report);
            ctx.setSourceCodeFile(tempFile);
            
            RuleSet ruleSet = ruleSetFactory.createRuleSet("rulesets/java/quickstart.xml");
            
            new SourceCodeProcessor(configuration).processSourceCode(
                new FileReader(tempFile),
                new RuleSets(ruleSet),
                ctx
            );
            
            for (RuleViolation violation : report.getViolations()) {
                Map<String, Object> result = new HashMap<>();
                result.put("line", violation.getBeginLine());
                result.put("description", violation.getDescription());
                result.put("rule", violation.getRule().getName());
                result.put("priority", violation.getRule().getPriority().getPriority());
                results.add(result);
            }
            
            tempFile.delete();
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Error during analysis: " + e.getMessage());
            results.add(error);
        }
        
        return results;
    }
    
    private File createTempFile(String content) throws IOException {
        File tempFile = File.createTempFile("code_analysis_", ".java");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }
        return tempFile;
    }
}