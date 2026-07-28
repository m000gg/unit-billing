pipeline {
    agent any

    environment {
        SERVICE_NAME = "unit-billing"
        APP_PORT     = "8080"
        PROD_HOST    = "opc@<PROD_VM_IP>"
    }

    stages {

        stage('Build') {
            steps {
                echo '=== Compiling unit-billing ==='
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                echo '=== Running unit tests ==='
                sh 'mvn test'
            }
        }

        stage('Package') {
            steps {
                echo '=== Packaging executable JAR ==='
                sh 'mvn package -DskipTests'
                echo 'Artifact built in target/unit-billing.jar'
            }
        }

        stage('Deploy to Test') {
            when {
                branch 'develop'
            }
            steps {
                echo '=== Deploy to TEST (local — Jenkins runs on this VM) ==='
                sh """
                    sudo systemctl stop ${SERVICE_NAME} || true
                    cp target/unit-billing.jar /opt/${SERVICE_NAME}/${SERVICE_NAME}.jar
                    sudo systemctl start ${SERVICE_NAME}
                    sudo systemctl status ${SERVICE_NAME} --no-pager
                """
            }
        }

        stage('Deploy to Prod') {
            when {
                branch 'main'
            }
            steps {
                echo '=== Deploy to PROD (remote via SCP + SSH) ==='
                sshagent(credentials: ['prod-ssh-key']) {
                    sh """
                        scp -o StrictHostKeyChecking=no target/unit-billing.jar ${PROD_HOST}:/opt/${SERVICE_NAME}/${SERVICE_NAME}.jar

                        ssh -o StrictHostKeyChecking=no ${PROD_HOST} "
                            sudo systemctl stop ${SERVICE_NAME} || true
                            sudo systemctl start ${SERVICE_NAME}
                            sudo systemctl status ${SERVICE_NAME} --no-pager
                        "
                    """
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