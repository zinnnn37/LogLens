# ============================================
# Jenkins LTS with JDK 21 and Pre-installed Plugins
# ============================================
FROM jenkins/jenkins:lts-jdk21

# Disable setup wizard since we're automating plugin installation
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"

# Copy plugins list file
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt

# Install plugins using jenkins-plugin-cli
# This runs during image build, so plugins are available on first startup
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Switch to root user for Docker-in-Docker capabilities
# This is required for Docker commands in Jenkins pipelines
USER root

# Install additional tools
# - unzip: Required for AWS CLI installation
# - curl: For health checks and API calls
# - ca-certificates: For HTTPS connections
RUN apt-get update && \
    apt-get install -y \
        unzip \
        curl \
        ca-certificates \
        gnupg \
        lsb-release && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set Jenkins home directory permissions
RUN chown -R jenkins:jenkins /var/jenkins_home

# Expose Jenkins web interface port
EXPOSE 8080

# Expose Jenkins agent port (for distributed builds)
EXPOSE 50000

# Jenkins will run as root but Jenkins processes run as jenkins user
# This allows Docker socket access while maintaining security
