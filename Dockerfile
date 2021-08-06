FROM findepi/graalvm:21.2.0-java11-polyglot

# install curl
RUN apt-get update && \
    apt-get install -y \
      curl && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

WORKDIR /app
COPY docker /

RUN graalpython -m venv /app/venv
RUN /app/venv/bin/pip install pymongo

ENV MICRONAUT_CONFIG_FILES=/app/application.yml VENV_HOME=/app/venv

# Create user
RUN useradd -ms /bin/bash akhq
# Chown to write configuration
RUN chown -R akhq /app
# Use the 'akhq' user
USER akhq
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./akhq"]
