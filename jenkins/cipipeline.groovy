pipeline {
    agent any
    
    tools {
        jdk "jdk17"
        maven "maven3"
    }

    parameters{
      string(name: 'IMAGE_TAG', defaultValue: '', description: 'tagging docker image')
    }

    environment {
        SCANNER_HOME=tool 'sonar-scanner'
        APP_NAME = "java-application"
        DOCKER_USR = "merajaprasd"
        DOCKERHUB_CREDENTIALS = credentials('docker-cred')
        IMAGE_TAG = "${params.IMAGE_TAG}"
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

        stage ('Unit Tests') {
            steps {
                sh 'mvn test'
            }
        }

        stage ('Static Analysis') {
            steps {
                echo "Running Software Composition Analysis using OWASP Dependency-Check ..."
                dependencyCheck additionalArguments: '--scan ./ --format XML', odcInstallation: 'DP-Check'
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }

        stage('Trivy FS Scan'){
            steps{
                sh 'trivy fs --format table -o fs.html .'
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
                echo "Running Quality Gates to verify the code quality...."
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

        stage('Publish Artifacts') {
            steps{
                echo "artifact publishing to Nexus Repo..."
                withMaven(globalMavenSettingsConfig: 'maven-settings', jdk: 'jdk17', maven: 'mvn', mavenSettingsConfig: '', traceability: true) {
                    sh 'mvn deploy'
                }
            }
        }

        stage ('Build Image') {
            steps {
                echo "Building Docker Image..."
                script {
                    sh "docker build -t ${DOCKER_USR}/${APP_NAME}:${IMAGE_TAG} ."
                }                    
            }
        }

        stage ('Scan Image') {
            steps {
                echo "Scanning Image for Vulnerabilities"
                sh "trivy image --format table -o trivyresults.html ${DOCKER_USR}/${APP_NAME}:${IMAGE_TAG}"
            }
        }

        stage('Docker Image Push') {
            steps{
                sh "docker images"
                sh "echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin"
                sh "docker push ${DOCKER_USR}/${APP_NAME}:${IMAGE_TAG}"
            }
        }

        stage ('Smoke Test') {
            steps {
                echo "Smoke Test the Image"
                sh "docker run -d --name smokerun -p 8080:8080 ${DOCKER_USR}/${APP_NAME}:${IMAGE_TAG}"
                sh "sleep 90; ./check.sh"
            }
        }

    }
    post {
        success {
            echo "Trigger CD Pipeline"
            build job: "java-application-cd", parameters: [string(name: 'IMAGE_TAG', value: "${params.IMAGE_TAG}")]
        }
    }
}
