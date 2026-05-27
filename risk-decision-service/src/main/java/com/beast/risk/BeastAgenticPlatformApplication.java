package com.beast.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(excludeName = {
        "org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration",
        "org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration"
})
@ComponentScan(basePackages = "com.beast.risk")
public class BeastAgenticPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeastAgenticPlatformApplication.class, args);

        System.out.println("\n✅ ✅ ✅ BEAST AGENTIC RISK PLATFORM STARTED ✅ ✅ ✅");
        System.out.println("   • Agent Ensemble Engine active");
        System.out.println("   • Risk Scoring Service (Kafka listener) ready");
        System.out.println("   • AI Review Agent (self-optimizing) scheduled");
        System.out.println("   • Ready for real interviews and demos!\n");
    }
}