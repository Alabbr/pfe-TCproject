pipeline {
    agent any

    environment {
        DOCKER_FRONTEND_IMAGE = 'pfe-frontend:latest'
        DOCKER_BACKEND_IMAGE = 'pfe-backend:latest'
    }

    stages {
        stage('Checkout') {
            steps {
                // Jenkins va automatiquement cloner le repo Github ici
                checkout scm
            }
        }

        stage('Build Backend (Maven)') {
            steps {
                dir('Backend/TC-Project') {
                    // Utilisation de bat car Jenkins sera sur Windows
                    bat 'mvnw.cmd clean package -DskipTests'
                }
            }
        }

        stage('Build Frontend (NPM)') {
            steps {
                dir('Frontend') {
                    bat 'npm install --legacy-peer-deps'
                    bat 'npm run build'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                // Build Backend Image
                dir('Backend/TC-Project') {
                    bat 'docker build -t %DOCKER_BACKEND_IMAGE% .'
                }
                // Build Frontend Image
                dir('Frontend') {
                    bat 'docker build -t %DOCKER_FRONTEND_IMAGE% .'
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                dir('Backend/k8s') {
                    // Applique les configurations k8s et force le redémarrage pour prendre les nouvelles images
                    bat 'kubectl apply -f .'
                    bat 'kubectl rollout restart deployment/backend-deployment -n pfe-app'
                    bat 'kubectl rollout restart deployment/frontend-deployment -n pfe-app'
                }
            }
        }
    }
}
