pipeline {
    agent any
    
    tools {
        jdk "jdk17"
        maven "maven3"
    }
    environment {
        SCANNER_HOME=tool 'sonar-scanner'
    }

    stages {
        stage ('clean WS'){
            steps {
                cleanWs()
            }
        }
        stage ('Checkout SCM') {
            steps {
                git branch: 'main' , url: 'https://github.com/merajaprasad/java-application.git'
            }
        }
        stage ('mvn compile') {
            steps {
                sh "mvn compile"
            }
        }
        stage ('mvn test') {
            steps {
                sh 'mvn test'
            }
        }
        stage ('Static Analysis') {
            steps {
                echo "Running Software Composition Analysis using OWASP Dependency-Check ..."
                dependencyCheck additionalArguments: '--scan ./', odcInstallation: 'DP-Check'
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }
        stage ('SAST') {
            steps {
                echo "Running Static application security testing using SonarQube Scanner ..."
                withSonarQubeEnv('sonar-server') {
                    sh '''$SCANNER_HOME/bin/sonar-scanner -Dsonar.projectName=blog-app -Dsonar.projectKey=blog-app \
                    -Dsonar.java.binaries=. '''
                }

            }
        }

        stage('QualityGates') {
            steps { 
                echo "Running Quality Gates to verify the code quality"
                script {
                timeout(time: 1, unit: 'MINUTES') {
                    def qg = waitForQualityGate()
                    if (qg.status != 'OK') {
                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                    }
                }
                }
            }
        }

        stage ('MVN Build') {
            steps {
                echo "Building Jar Component ..."
                sh "mvn clean package"
            }
        }

        stage ('Build Image') {
            steps {
                echo "Build Docker Image"
                sh "docker build -t merajaprasd/java-application:latest ."
            }
        }
        stage ('Scan Image') {
            steps {
                echo "Scanning Image for Vulnerabilities"
                sh "trivy image --format table -o trivyresults.html merajaprasd/java-application:latest"
            }
        }
        stage ('Smoke Test') {
            steps {
                echo "Smoke Test the Image"
                sh "docker run -d --name smokerun -p 8080:8080 merajaprasd/java-application:latest"
                sh "sleep 90; ./check.sh"
            }
        }

    }

}
