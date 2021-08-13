FROM adoptopenjdk/openjdk11:latest

RUN apt-get update && \
    apt-get install -y sqlite3

#Install Cron
RUN apt-get update
RUN apt-get -y install cron

# Add crontab file in the cron directory
ADD crontab /etc/cron.d/billing

# Give execution rights on the cron job
RUN chmod 0644 /etc/cron.d/billing

# Create the log file to be able to run tail
RUN touch /var/log/cron.log

COPY . /anteus
WORKDIR /anteus

EXPOSE 7000
# When the container starts: build, test and run the app.
CMD ./gradlew build && ./gradlew test && ./gradlew run
