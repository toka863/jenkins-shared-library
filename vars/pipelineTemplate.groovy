def call(Map config = [:]) {

    pipeline {

        agent any

        tools {
            maven 'maven-tag'
            jdk 'JDK17-ITI'
        }

        environment {
            IMAGE_NAME = "${config.imageName}"
            IMAGE_TAG  = "${config.imageTag ?: 'latest'}"
            PORT = "${config.port}"
            AWS_REGION = "${config.region ?: 'us-east-1'}"
            ECR_REPO   = "${config.ecrRepo}"
            ACCOUNT_ID = "${config.accountId}"
        }

        stages {

            stage('Clone') {
                steps {
                    git url: config.repo, branch: 'main'
                }
            }

            stage('Compile') {
                steps {
                    sh "mvn clean compile"
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

            stage('Login to ECR') {
                steps {
                    withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: 'aws-creds'
                    ]]) {
                        sh '''
                        aws ecr get-login-password --region $AWS_REGION \
                        | docker login --username AWS --password-stdin $ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
                        '''
                    }
                }
            }

            stage('Tag Image') {
                steps {
                    sh '''
                    docker tag $IMAGE_NAME:$IMAGE_TAG \
                    $ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG
                    '''
                }
            }

            stage('Push Image') {
                steps {
                    sh '''
                    docker push $ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG
                    '''
                }
            }

            stage('Deploy') {
                steps {
                    sh '''
                    docker run -d -p $PORT:8080 \
                    $ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG
                    '''
                }
            }
        }
    }
}
