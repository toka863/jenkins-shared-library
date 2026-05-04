def call(Map config = [:]) {

    pipeline {

        agent any

        tools {
            maven 'mvn-iti'
            jdk 'JDK17-ITI'
        }

        environment {
            IMAGE_NAME = config.imageName
            IMAGE_TAG  = "latest"
            PORT       = config.port
        }

        stages {

            stage('Clone') {
                steps {
                    git url: config.repo
                }
            }

            stage('Compile') {
                steps {
                    sh "mvn clean compile"
                }
            }

            stage('Test') {
                steps {
                    sh "mvn test"
                }
            }

            stage('Package') {
                steps {
                    sh "mvn package -DskipTests"
                }
            }

            stage('Build Docker Image') {
                steps {
                    sh "docker build -t $IMAGE_NAME:$IMAGE_TAG ."
                }
            }

            stage('Push Image') {
                steps {
                    sh "echo pushing image..."
                    // docker push لو عندك registry
                }
            }

            stage('Deploy') {
                steps {
                    sh "docker run -d -p $PORT:8080 $IMAGE_NAME:$IMAGE_TAG"
                }
            }
        }
    }
}
