pipeline {
    agent any

    environment {
        REGISTRY = "registrycont.azurecr.io"
        IMAGE_NAME = "unit-billing"
        ACR_CREDS_ID = "azure-acr-creds"
    }

    stages {

        stage('Install Parent POM') {
            steps {
                echo '=== Installing Root POM to Jenkins Cache ==='
                sh 'mvn clean install -N'
            }
        }

        stage('Build Shared') {
            steps {
                dir('packages/shared-core') {
                    echo '=== Building shared-core ==='
                    sh 'mvn clean install -DskipTests'
                }
            }
        }

        stage('Build') {
            parallel {

                stage('Admin App Build') {
                    steps {
                        dir('apps/admin/backend') {
                            echo '=== Admin App Build process ==='
                            sh 'mvn clean compile'
                        }
                    }
                }

                stage('Client App Build') {
                    steps {
                        dir('apps/client/backend') {
                            echo '=== Client App Build process ==='
                            sh 'mvn clean compile'
                        }
                    }
                }
            }
        }

        stage('Tests') {
            parallel {

                stage('Test Admin') {
                    steps {
                        dir('apps/admin/backend') {
                            sh 'mvn test'
                        }
                    }
                }

                stage('Test Client') {
                    steps {
                        dir('apps/client/backend') {
                            sh 'mvn test'
                        }
                    }
                }
            }
        }

        stage('Package') {
            parallel {

                stage('Package Admin') {
                    steps {
                        dir('apps/admin/backend') {
                            echo '=== Final JAR-File Packaging for Admin App ==='
                            sh 'mvn package -DskipTests'
                            echo 'Local artifact successfully built in target/ folder'
                        }
                    }
                }

                stage('Package Client') {
                    steps {
                        dir('apps/client/backend') {
                            echo '=== Final JAR-File Packaging for Client App ==='
                            sh 'mvn package -DskipTests'
                            echo 'Local artifact successfully built in target/ folder'
                        }
                    }
                }
            }
        }


        stage('Docker Build & Push') {
            parallel {

                stage('Push Admin') {
                    steps {
                        dir('apps/admin/backend') {
                            echo "=== Docker Build & Push of Admin App (Branch: ${env.BRANCH_NAME}) ==="

                            //take credentials from jenkins
                            withCredentials([usernamePassword(credentialsId: env.ACR_CREDS_ID, passwordVariable: 'ACR_PASS', usernameVariable: 'ACR_USER')]) {

                                //login to Azure Container Registry
                                sh "echo \$ACR_PASS | docker login ${REGISTRY} -u \$ACR_USER --password-stdin"

                                //build image with branch-specific tags
                                sh "docker build -t ${REGISTRY}/${IMAGE_NAME}-admin:${env.BRANCH_NAME}-${BUILD_NUMBER} ."
                                sh "docker build -t ${REGISTRY}/${IMAGE_NAME}-admin:${env.BRANCH_NAME}-latest ."

                                //send to Register
                                sh "docker push ${REGISTRY}/${IMAGE_NAME}-admin:${env.BRANCH_NAME}-${BUILD_NUMBER}"
                                sh "docker push ${REGISTRY}/${IMAGE_NAME}-admin:${env.BRANCH_NAME}-latest"

                                //clean up
                                sh "docker rmi ${REGISTRY}/${IMAGE_NAME}-admin:${env.BRANCH_NAME}-${BUILD_NUMBER}"
                                sh "docker rmi ${REGISTRY}/${IMAGE_NAME}-admin:${env.BRANCH_NAME}-latest"
                            }
                        }
                    }
                }

                stage('Push Client') {
                    steps {
                        dir('apps/client/backend') {
                            echo "=== Docker Build & Push of Client App (Branch: ${env.BRANCH_NAME}) ==="

                            //take credentials from jenkins
                            withCredentials([usernamePassword(credentialsId: env.ACR_CREDS_ID, passwordVariable: 'ACR_PASS', usernameVariable: 'ACR_USER')]) {

                                //login to Azure Container Registry
                                sh "echo \$ACR_PASS | docker login ${REGISTRY} -u \$ACR_USER --password-stdin"

                                //build image with branch-specific tags
                                sh "docker build -t ${REGISTRY}/${IMAGE_NAME}-client:${env.BRANCH_NAME}-${BUILD_NUMBER} ."
                                sh "docker build -t ${REGISTRY}/${IMAGE_NAME}-client:${env.BRANCH_NAME}-latest ."

                                //send to Register
                                sh "docker push ${REGISTRY}/${IMAGE_NAME}-client:${env.BRANCH_NAME}-${BUILD_NUMBER}"
                                sh "docker push ${REGISTRY}/${IMAGE_NAME}-client:${env.BRANCH_NAME}-latest"


                                //clean up
                                sh "docker rmi ${REGISTRY}/${IMAGE_NAME}-client:${env.BRANCH_NAME}-${BUILD_NUMBER}"
                                sh "docker rmi ${REGISTRY}/${IMAGE_NAME}-client:${env.BRANCH_NAME}-latest"
                            }
                        }
                    }
                }

            }
        }

        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'staging-db-creds', passwordVariable: 'DB_PASS', usernameVariable: 'DB_USER')]) {
                    sshagent(credentials: ['prod-ssh-key']) {
                        sh """
                        ssh -o StrictHostKeyChecking=no azureuser@98.70.24.6 "
                            echo '=== Deploy to STAGING from Branch DEVELOP ==='

                            echo '--- Deploying Admin App (Staging) ---'
                            docker pull registrycont.azurecr.io/unit-billing-admin:develop-latest
                            
                            docker stop admin-app-staging || true
                            docker rm admin-app-staging || true
                            
                            docker run -d --restart unless-stopped \\
                              --name admin-app-staging \\
                              -m 512m \\
                              -p 9080:8080 \\
                              -e SPRING_DATASOURCE_URL='jdbc:postgresql://unit-billing-postgres-server.postgres.database.azure.com:5432/staging-db-postgres?sslmode=require' \\
                              -e SPRING_DATASOURCE_USERNAME='${env.DB_USER}' \\
                              -e SPRING_DATASOURCE_PASSWORD='${env.DB_PASS}' \\
                              registrycont.azurecr.io/unit-billing-admin:develop-latest

                            echo '--- Deploying Client App (Staging) ---'
                            docker pull registrycont.azurecr.io/unit-billing-client:develop-latest
                            
                            docker stop client-app-staging || true
                            docker rm client-app-staging || true
                            
                            docker run -d --restart unless-stopped \\
                              --name client-app-staging \\
                              -m 512m \\
                              -p 9081:8081 \\
                              -e SPRING_DATASOURCE_URL='jdbc:postgresql://unit-billing-postgres-server.postgres.database.azure.com:5432/staging-db-postgres?sslmode=require' \\
                              -e SPRING_DATASOURCE_USERNAME='${env.DB_USER}' \\
                              -e SPRING_DATASOURCE_PASSWORD='${env.DB_PASS}' \\
                              registrycont.azurecr.io/unit-billing-client:develop-latest
                             
                            docker image prune -f
                            echo '=== Staging Deploy successfully ended! ==='
                        "
                        """
                    }
                }
            }
        }

        stage('Deploy to Prod') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'prod-db-creds', passwordVariable: 'DB_PASS', usernameVariable: 'DB_USER')]) {
                    sshagent(credentials: ['prod-ssh-key']) {
                        sh """
                        ssh -o StrictHostKeyChecking=no azureuser@98.70.24.6 "
                            echo '=== Deploy to PROD from Branch MAIN ==='

                            echo '--- Deploying Admin App (Prod) ---'
                            docker pull registrycont.azurecr.io/unit-billing-admin:main-latest
                            
                            docker stop admin-app || true
                            docker rm admin-app || true
                            
                            docker run -d --restart unless-stopped \\
                              --name admin-app \\
                              -m 512m \\
                              -p 8080:8080 \\
                              -e SPRING_DATASOURCE_URL='jdbc:postgresql://unit-billing-postgres-server.postgres.database.azure.com:5432/prod-db-postgres?sslmode=require' \\
                              -e SPRING_DATASOURCE_USERNAME='${env.DB_USER}' \\
                              -e SPRING_DATASOURCE_PASSWORD='${env.DB_PASS}' \\
                              registrycont.azurecr.io/unit-billing-admin:main-latest

                            echo '--- Deploying Client App (Prod) ---'
                            docker pull registrycont.azurecr.io/unit-billing-client:main-latest
                            
                            docker stop client-app || true
                            docker rm client-app || true
                            
                            docker run -d --restart unless-stopped \\
                              --name client-app \\
                              -m 512m \\
                              -p 8081:8081 \\
                              -e SPRING_DATASOURCE_URL='jdbc:postgresql://unit-billing-postgres-server.postgres.database.azure.com:5432/prod-db-postgres?sslmode=require' \\
                              -e SPRING_DATASOURCE_USERNAME='${env.DB_USER}' \\
                              -e SPRING_DATASOURCE_PASSWORD='${env.DB_PASS}' \\
                              registrycont.azurecr.io/unit-billing-client:main-latest
                             
                            docker image prune -f
                            echo '=== Prod Deploy successfully ended! ==='
                        "
                        """
                    }
                }
            }
        }

    }

    post {
        success {
            echo 'Success!'
        }
        failure {
            echo 'Build failed. Please check the logs for details.'
        }
    }

}