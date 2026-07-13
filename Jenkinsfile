pipeline {
    agent { label 'kpaper-jdk25' }

    options {
        skipDefaultCheckout(true)
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '30'))
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('API compatibility') {
            steps { sh './gradlew apiCheck --stacktrace --no-daemon' }
        }
        stage('Build') {
            steps { sh './gradlew build --stacktrace --no-daemon' }
        }
    }

    post {
        always { junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml' }
        cleanup { deleteDir() }
    }
}
