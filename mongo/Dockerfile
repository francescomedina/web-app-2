FROM mongo:latest
RUN mkdir /config
WORKDIR /config
COPY wait-for-it.sh .
COPY mongo-setup.js .
COPY mongo-setup.sh .
COPY init-user.json /init-user.json
COPY init-wallet.json /init-wallet.json
COPY init-warehouse.json /init-warehouse.json
COPY init-product.json /init-product.json
COPY init-product-availability.json /init-product-availability.json
RUN chmod +x /config/wait-for-it.sh
RUN chmod +x /config/mongo-setup.sh
CMD [ "bash", "-c", "/config/wait-for-it.sh mongodb-primary:27017 -- /config/mongo-setup.sh \
    && mongoimport --host mongodb-primary --db user-db --collection user --drop --file /init-user.json --jsonArray \
    && mongoimport --host mongodb-primary --db wallet-db --collection wallet --drop --file /init-wallet.json --jsonArray \
    && mongoimport --host mongodb-primary --db warehouse-db --collection warehouse --drop --file /init-warehouse.json --jsonArray \
    && mongoimport --host mongodb-primary --db warehouse-db --collection product --drop --file /init-product.json --jsonArray \
    && mongoimport --host mongodb-primary --db warehouse-db --collection product-availability --drop --file /init-product-availability.json --jsonArray"]