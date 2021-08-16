FROM adoptopenjdk/openjdk11:latest

RUN apt-get update && \
    apt-get install -y sqlite3

#Install Python
RUN apt-get install -y python
RUN apt-get install -y python3-pip

#Install mjson tool
RUN pip install mjson
#
##Install Cron and vim and supervisor
RUN apt-get install cron -y && apt-get install vim -y && apt-get install supervisor -y

# Add crontab file in the cron directory
ADD cronjob /etc/cron.d/billing

# Give execution rights on the cron job
RUN chmod 0644 /etc/cron.d/billing

# Apply cron job
RUN crontab /etc/cron.d/*

# Create the log file to be able to run tail
RUN touch /var/log/cron.log

COPY . /antaeus
WORKDIR /antaeus

# Give rights on the billing-cron.sh
RUN chmod 0744 ./billing-cron.sh

# Copy supervisor conf
COPY supervisord.conf /etc/supervisor/

EXPOSE 7000
# When the container starts: run supervisor
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/supervisord.conf"]