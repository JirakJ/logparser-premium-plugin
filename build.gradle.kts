import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "com.jakubjirak"

// Auto-increment version on each build (SemVer: MAJOR.MINOR.PATCH)
val buildNumber = System.getenv("BUILD_NUMBER") ?: LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
version = "1.0.0-build.$buildNumber"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.apache.lucene:lucene-core:9.8.0")
    implementation("org.apache.lucene:lucene-queryparser:9.8.0")
    implementation("org.apache.lucene:lucene-analyzers-common:8.11.2")
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.yaml:snakeyaml:2.2")
    
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

intellij {
    version.set("2024.2")
    type.set("IC") // IntelliJ IDEA Community Edition
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("242")
        untilBuild.set("")  // No upper limit
        
        pluginDescription.set("""
            <h1>LogParser Pro - Professional Log Analysis for JetBrains IDEs</h1>
            
            <h2>🆓 Free Features:</h2>
            <ul>
                <li>Syntax highlighting for JSON, plaintext, Apache/Nginx logs</li>
                <li>Log level filtering (ERROR, WARN, INFO, DEBUG)</li>
                <li>Keyword search and basic filtering</li>
                <li>Auto-detect common patterns (Spring Boot, Node.js, Python)</li>
                <li>Stack trace folding</li>
            </ul>
            
            <h2>💎 Premium Features ($79/year):</h2>
            <ul>
                <li><b>Transaction/Correlation ID Tracking</b> - Cross-pod log correlation</li>
                <li><b>Multi-source Aggregation</b> - Kubernetes, OpenShift, SSH, Docker</li>
                <li><b>Real-time Multi-source Monitoring</b> - Live tail from 10+ sources</li>
                <li><b>AI Pattern Recognition</b> - ML-powered anomaly detection</li>
                <li><b>Advanced Analytics</b> - Dashboards, heatmaps, metrics</li>
                <li><b>Team Collaboration</b> - Shared queries, annotations</li>
                <li><b>Unlimited Performance</b> - Handle GB+ files effortlessly</li>
            </ul>
            
            <p>Stop jumping between kubectl logs, SSH sessions, and grep. See your entire distributed transaction in one place.</p>
            
            <p><a href="https://logparser.dev">Learn more at logparser.dev</a></p>
        """.trimIndent())
        
        changeNotes.set("""
            <h3>1.0.0 - Initial Release</h3>
            <ul>
                <li>Free tier: Basic log viewing and filtering</li>
                <li>Premium tier: Transaction correlation tracking (30-day trial)</li>
                <li>Multi-source support: Kubernetes, Docker, SSH</li>
                <li>High-performance indexing with Lucene</li>
            </ul>
        """.trimIndent())
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(
            (project.findProperty("publishToken") as? String)
                ?: System.getenv("PUBLISH_TOKEN")
                ?: ""
        )
    }
    
    // Disable searchable options for paid plugins
    buildSearchableOptions {
        enabled = false
    }
    
    test {
        useJUnitPlatform()
    }
}
