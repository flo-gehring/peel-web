FROM mcr.microsoft.com/devcontainers/java:25-bookworm

# Install Node.js 20 LTS using the pre-installed nvm
ARG NODE_VERSION="20"
RUN bash -c "source /usr/local/share/nvm/nvm.sh && nvm install ${NODE_VERSION}"

# Set the default shell to bash for user 'vscode'
ENV SHELL=/bin/bash
