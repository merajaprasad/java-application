pipeline {
    agent any

    parameters{
      string(name: 'IMAGE_TAG', defaultValue: '', description: 'Please provide the Image Tag to Deploy in prod?')
    }


    stages {
        stage ("Validation") {
            steps {
                script {
                    if (params.IMAGE_TAG == '') {
                        echo "ERROR: Please provide the latest IMAGE_TAG to deploy in Prod."
                        error ("Please provide the latest IMAGE_TAG to deploy in Prod.")
                    }
                }
            }
        } 
        stage ('clean WS'){
            steps {
                cleanWs()
            }
        }
        stage ("Checkout Code") {
            steps {
                git branch: 'main', url: 'https://github.com/merajaprasad/java-application.git'
            }
        }

        stage ("Update: Kubernetes Manifests") {
            steps {
                script {
                    dir('kubernetes') {
                        sh "sed -i 's/java-application.*/java-application:${params.IMAGE_TAG}/g' deployment.yml"
                    }
                }
            }
        }

        stage ("Git: Code update and push") {
            steps {
                script {
                    withCredentials([gitUsernamePassword(credentialsId: 'Github-cred', gitToolName: 'Default')]) {
                        sh '''
                        echo "Adding the Changes: "
                        git add .

                        echo "Commiting changes: "
                        git commit -m "updating image tag in deploymentfile"
                        
                        echo "Pushing changes to github: "
                        git push https://github.com/merajaprasad/java-application.git main
                    '''
                    }
                }
            }
        }
    }
}
