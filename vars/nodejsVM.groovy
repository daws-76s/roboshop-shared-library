def call(Map configMap){
    pipeline {
        agent {
            node {
                label 'AGENT-1'
            }
        }
        environment { 
            packageVersion = ''
            // can maintain in pipeline globals
            //nexusURL = '172.31.5.95:8081'
        }
        options {
            timeout(time: 1, unit: 'HOURS')
            disableConcurrentBuilds()
        }
        parameters {
            // string(name: 'PERSON', defaultValue: 'Mr Jenkins', description: 'Who should I say hello to?')

            // text(name: 'BIOGRAPHY', defaultValue: '', description: 'Enter some information about the person')

            booleanParam(name: 'Deploy', defaultValue: false, description: 'Toggle this value')

            // choice(name: 'CHOICE', choices: ['One', 'Two', 'Three'], description: 'Pick something')

            // password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
        }
        // build
        stages {
            stage('Get the version') {
                steps {
                    script {
                        def packageJson = readJSON file: 'package.json'
                        packageVersion = packageJson.version
                        echo "application version: $packageVersion"
                    }
                }
            }
            stage('Install dependencies') {
                steps {
                    sh """
                        npm install
                    """
                }
            }
            stage('Unit tests') {
                steps {
                    sh """
                        echo "unit tests will run here"
                    """
                }
            }
            stage('Sonar Scan'){
                steps{
                    sh """
                        echo "usually command here is sonar-scanner"
                        echo "sonar scan will run here"
                    """
                }
            }
            stage('Build') {
                steps {
                    sh """
                        ls -la
                        zip -q -r ${configMap.component}.zip ./* -x ".git" -x "*.zip"
                        ls -ltr
                    """
                }
            }
            stage('Publish Artifact') {
                steps {
                    nexusArtifactUploader(
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        nexusUrl: pipelineGlobals.nexusURL(),
                        groupId: 'com.roboshop',
                        version: "${packageVersion}",
                        repository: "${configMap.component}",
                        credentialsId: 'nexus-auth',
                        artifacts: [
                            [artifactId: "${configMap.component}",
                            classifier: '',
                            file: "${configMap.component}.zip",
                            type: 'zip']
                        ]
                    )
                }
            }
            stage('Deploy') {
                when {
                    expression{
                        params.Deploy
                    }
                }
                steps {
                    script {
                            def params = [
                                string(name: 'version', value: "$packageVersion"),
                                string(name: 'environment', value: "dev")
                                booleanParam(name: 'Create', value: "${params.Deploy}")
                            ]
                            build job: "../${configMap.component}-deploy", wait: true, parameters: params
                        }
                }
            }
        }
        // post build
        post { 
            always { 
                echo 'I will always say Hello again!'
                deleteDir()
            }
            failure { 
                echo 'this runs when pipeline is failed, used generally to send some alerts'
            }
            success{
                echo 'I will say Hello when pipeline is success'
            }
        }
    }
}