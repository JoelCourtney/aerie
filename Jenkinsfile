def getDockerCompatibleTag(tag){
    def fixedTag = tag.replaceAll('\\+', '-')
    return fixedTag
}

def getDockerImageName(folder){
    files = findFiles(glob: "${DOCKERFILE_PATH}/*.*")
    def list = []

    for (def file : files) {
        def token = (file.getName()).tokenize('.')
        if (token.size() > 1 && token[1] == "Dockerfile") {
            list.push(token[0])
        } else {
            println("File not added " + token[0])
        }
    }
    for (def item : list) {
        println("File name = "+item)
    }

    return list

}

def getAWSTag(tag){
    if (tag ==~ /release/) {
        return "release"
    }
    if (tag ==~ /develop/) {
        return "develop"
    }
    if (tag ==~ /staging/) {
        return "staging"
    }
    return "testing"
}

def getArtifactoryUrl() {
    echo "Choosing an Artifactory port based off of branch name: $GIT_BRANCH"

    if (GIT_BRANCH ==~ /release/){
        echo "Publishing to 16002-STAGE-LOCAL"
        return "cae-artifactory.jpl.nasa.gov:16002"
    }
    else {
        echo "Publishing to 16001-DEVELOP-LOCAL"
        return "cae-artifactory.jpl.nasa.gov:16001"
    }
}

def buildImages = []
def imageNames = []

pipeline {

    agent {
        // NOTE: DEPLOY WILL ONLY WORK WITH Coronado SERVER SINCE AWS CLI VERSION 2 IS ONLY INSTALLED ON THAT SERVER.
        // label 'coronado || Pismo || San-clemente || Sugarloaf'
        label 'coronado'
    }

    environment {
        ARTIFACT_TAG = "${GIT_BRANCH}"
        ARTIFACTORY_URL = "${getArtifactoryUrl()}"
        AWS_ACCESS_KEY_ID = credentials('aerie-aws-access-key')
        AWS_DEFAULT_REGION = 'us-gov-west-1'
        AWS_ECR = "448117317272.dkr.ecr.us-gov-west-1.amazonaws.com"
        AWS_SECRET_ACCESS_KEY = credentials('aerie-aws-secret-access-key')
        BUCK_HOME = "/usr/local/bin"
        BUCK_OUT = "${env.WORKSPACE}/buck-out"
        DOCKER_TAG = "${getDockerCompatibleTag(ARTIFACT_TAG)}"
        AWS_TAG = "${getAWSTag(DOCKER_TAG)}"
        DOCKERFILE_DIR = "${env.WORKSPACE}/scripts/dockerfiles"
        JDK11_HOME = "/usr/lib/jvm/java-11-openjdk"
        LD_LIBRARY_PATH = "/usr/local/lib64:/usr/local/lib:/usr/lib64:/usr/lib"
        WATCHMAN_HOME = "/opt/watchman"
        ARTIFACT_PATH = "${ARTIFACTORY_URL}/gov/nasa/jpl/ammos/mpsa/aerie"
        AWS_ECR_PATH = "${AWS_ECR}/aerie"
        DOCKERFILE_PATH = "scripts/dockerfiles"
    }

    stages {
        stage('Setup'){
            steps {
                echo "Printing environment variables..."
                sh "env | sort"
            }
        }

        stage ('Build') {
            steps {
                echo "Building $ARTIFACT_TAG..."
                script {
                    def statusCode = sh returnStatus: true, script:
                    """
                    echo "Building all build targets"
                    buck build //...
                    """
                    if (statusCode > 0) {
                        error "Failure in Build stage."
                    }
                }
            }
        }

        stage ('Test') {
            steps {
                echo "TODO: Run tests via BUCK"
                // sh "buck test //..."
            }
        }

        stage ('Archive') {
            when {
                expression { GIT_BRANCH ==~ /(develop|staging|release)/ }
            }
            steps {
                // TODO: Publish Merlin-SDK.jar to Maven/Artifactory

                echo 'Publishing JARs to Artifactory...'
                script {
                    def statusCode = sh returnStatus: true, script:
                    """
                    # Tar up all build artifacts.
                    find . -name "*.jar" ! -path "**/third-party/*" ! -path "**/.buckd/*"  ! -path "**/*test*/*" ! -name "*abi.jar" | tar -czf aerie-${ARTIFACT_TAG}.tar.gz -T -
                    """

                    if (statusCode > 0) {
                        error "Failure in Archive stage."
                    }
                }

                script {
                    try {
                        def server = Artifactory.newServer url: 'https://cae-artifactory.jpl.nasa.gov/artifactory', credentialsId: '9db65bd3-f8f0-4de0-b344-449ae2782b86'
                        def uploadSpec =
                        '''
                        {
                            "files": [
                                {
                                    "pattern": "aerie-${ARTIFACT_TAG}.tar.gz",
                                    "target": "general-develop/gov/nasa/jpl/ammos/mpsa/aerie/",
                                    "recursive":false
                                }
                            ]
                        }
                        '''
                        def buildInfo = server.upload spec: uploadSpec
                        server.publishBuildInfo buildInfo
                    } catch (Exception e) {
                        println("Publishing to Artifactory failed with exception: ${e.message}")
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage ('Docker') {
            when {
                expression { GIT_BRANCH ==~ /(AERIE-664-Deployment-Problem|develop|staging|release)/ }
            }
            steps {
                script {
                    imageNames = getDockerImageName(env.DOCKERFILE_DIR)
                    docker.withRegistry('https://cae-artifactory.jpl.nasa.gov:16001', '9db65bd3-f8f0-4de0-b344-449ae2782b86') {
                        for (def name: imageNames) {
                            def tag_name="$ARTIFACT_PATH/$name:$DOCKER_TAG"
                            def image = docker.build("${tag_name}", "--progress plain -f ${DOCKERFILE_PATH}/${name}.Dockerfile --rm ." )
                            image.push()
                            buildImages.push(tag_name)
                        }
                    }

                }

            }
        }

        stage('Deploy') {
            when {
                expression { GIT_BRANCH ==~ /(AERIE-664-Deployment-Problem|develop|staging|release)/ }
            }
            steps {
                echo 'Deployment stage started...'
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'mpsa-aws-test-account']]) {
                    script{
                        echo 'Logging out docker'
                        sh 'docker logout || true'
                        echo 'Logging into ECR'
                        sh ('aws ecr get-login-password | docker login --username AWS --password-stdin https://$AWS_ECR')

                        def filenames = getDockerImageName(env.DOCKERFILE_DIR)
                        docker.withRegistry(AWS_ECR){
                            for (def name: imageNames) {
                                def old_tag_name="$ARTIFACT_PATH/$name:$DOCKER_TAG"
                                def new_tag_name="$AWS_ECR_PATH/$name:$AWS_TAG"
                                def changeTagCmd = "docker tag $old_tag_name $new_tag_name"
                                def pushCmd = "docker push $new_tag_name"

                                // retag the image and push to aws
                                sh changeTagCmd
                                sh pushCmd
                                buildImages.remove(old_tag_name)
                                buildImages.push(new_tag_name)
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            println(buildImages)
            for (def image: buildImages) {
                def removeCmd = "docker rmi $image"
                sh removeCmd
            }
            echo 'Cleaning up images'
            sh "docker image prune -f"

            echo 'Logging out docker'
            sh 'docker logout || true'
        }

        unstable {
            emailext subject: "Jenkins UNSTABLE: ${env.JOB_BASE_NAME} #${env.BUILD_NUMBER}",
            body: """
                <p>Jenkins job unstable (failed tests): <br> <a href=\"${env.BUILD_URL}\">${env.JOB_NAME} #${env.BUILD_NUMBER}</a></p>
            """,
            mimeType: 'text/html',
            recipientProviders: [[$class: 'FailingTestSuspectsRecipientProvider']]
        }

        failure {
            emailext subject: "Jenkins FAILURE: ${env.JOB_BASE_NAME} #${env.BUILD_NUMBER}",
            body: """
                <p>Jenkins job failure: <br> <a href=\"${env.BUILD_URL}\">${env.JOB_NAME} #${env.BUILD_NUMBER}</a></p>
            """,
            mimeType: 'text/html',
            recipientProviders: [[$class: 'CulpritsRecipientProvider']]
        }
    }
}
