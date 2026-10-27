def dockerGid = ''
node {
    dockerGid = sh(script: "stat -c '%g' /var/run/docker.sock", returnStdout: true).trim()
}

pipeline {
    agent {
        docker {
            image 'maven:3.10.0-sapmachine-25'
            args "--user 0:0 -v /var/run/docker.sock:/var/run/docker.sock --group-add ${dockerGid} -v maven-repository:/root/.m2"
        }
    }

    parameters {
        choice(
                name: 'DEPLOY_TARGET',
                choices: ['NONE', 'TEST', 'PROD'],
                description: 'Deployment target. Select NONE to build, test, and package without deploying.'
        )
    }

    environment {
        SERVICE_NAME = "unit-billing"
        APP_PORT     = "8080"
        APP_JAR_GLOB = "unit-billing-core/target/unit-billing-*.jar"
        MAVEN_ARGS   = "-Dmaven.repo.local=${WORKSPACE}/.m2/repository"
    }

    stages {

        stage('Build') {
            steps {
                echo '=== Compiling unit-billing (all modules) ==='
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                echo '=== Running unit tests (all modules) ==='
                sh 'mvn test'
            }
        }

        stage('Package') {
            steps {
                echo '=== Packaging executable JAR ==='
                sh 'mvn package -DskipTests'
                echo 'Artifact built in unit-billing-core/target/unit-billing-<version>.jar'
            }
        }

        stage('Deploy to Test') {
            when {
                expression {
                    params.DEPLOY_TARGET == 'TEST'
                }
            }
            steps {
                echo '=== Deploy to TEST (remote via SCP + SSH) ==='
                deploy('test', 'test-host', 'test-known-hosts', 'test-ssh-key')
            }
        }

        stage('Deploy to Prod') {
            when {
                expression {
                    params.DEPLOY_TARGET == 'PROD'
                }
            }
            steps {
                echo '=== Deploy to PROD (remote via SCP + SSH) ==='
                deploy('prod', 'prod-host', 'prod-known-hosts', 'prod-ssh-key')
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

def deploy(String profile, String hostCredentialId, String knownHostsCredentialId, String sshCredentialId) {
    withCredentials([
            string(credentialsId: hostCredentialId, variable: 'DEPLOY_HOST'),
            file(credentialsId: knownHostsCredentialId, variable: 'KNOWN_HOSTS')
    ]) {
        sshagent(credentials: [sshCredentialId]) {
            sh """
                echo "=== Running Flyway Repair for profile: ${profile} ==="
                mvn flyway:repair -pl unit-billing-core -Dspring.profiles.active=${profile} || true
            """

            sh '''
                scp -o UserKnownHostsFile="$KNOWN_HOSTS" -o StrictHostKeyChecking=yes \
                    $APP_JAR_GLOB "$DEPLOY_HOST:/opt/$SERVICE_NAME/$SERVICE_NAME.jar.new"

                ssh -o UserKnownHostsFile="$KNOWN_HOSTS" -o StrictHostKeyChecking=yes "$DEPLOY_HOST" \
                    "sudo systemctl stop '$SERVICE_NAME' && \
                     sudo mv '/opt/$SERVICE_NAME/$SERVICE_NAME.jar.new' '/opt/$SERVICE_NAME/$SERVICE_NAME.jar' && \
                     sudo chown 'unitbilling:unitbilling' '/opt/$SERVICE_NAME/$SERVICE_NAME.jar' && \
                     sudo systemctl start '$SERVICE_NAME' && \
                     sudo systemctl status '$SERVICE_NAME' --no-pager"
            '''
        }
    }
}