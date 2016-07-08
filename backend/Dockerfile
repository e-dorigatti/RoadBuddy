FROM postgres:9.5

RUN DEBIAN_FRONTEND=noninteractive apt-get update && apt-get install -y vim wget sudo
RUN sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt jessie-pgdg main" >> /etc/apt/sources.list'
RUN wget --quiet -O - http://apt.postgresql.org/pub/repos/apt/ACCC4CF8.asc | apt-key add -
RUN DEBIAN_FRONTEND=noninteractive apt-get update && apt-get install -y postgresql-9.5-postgis-2.2
VOLUME /var/lib/postgresql/data