FROM mongo:latest
RUN mkdir /config
WORKDIR /config
COPY wait-for-it.sh .
COPY mongo-setup.js .
COPY mongo-setup.sh .
COPY init-user.json /init-user.json
COPY init-wallet.json /init-wallet.json
RUN chmod +x /config/wait-for-it.sh
RUN chmod +x /config/mongo-setup.sh
CMD [ "bash", "-c", "/config/wait-for-it.sh mongodb-primary:27017 -- /config/mongo-setup.sh \
    && mongoimport --host mongodb-primary --db user-db --collection user --file /init-user.json --jsonArray \
    && mongoimport --host mongodb-primary --db wallet-db --collection wallet --file /init-wallet.json --jsonArray"]