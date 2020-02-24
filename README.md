# Obtain a new lease from Vault when the current one expired without rebooting Spring Boot Applications

***Notice: The application is created for the propose of technical demonstration. The solution should never be used in production environment.***

# Acknowledgement
The application exists because of a prestigious blog post named  [Heavy Rotation of Relational Hashicorp Vault Database Secrets in Spring](https://secrets-as-a-service.com/posts/hashicorp-vault/rotate-dynamic-relational-database-connection-in-spring-at-runtime/).

The blog post gives the fourth solution to resolve a spring vault issue that the database credentials will not be rotated after the lease expired its max_ttl. For detail about the issue, pls refer to [here](https://github.com/spring-cloud/spring-cloud-vault/issues/256).

For the former three solutions, please refer to [Hashicorp Vault max_ttl Killed My Spring App](https://secrets-as-a-service.com/posts/hashicorp-vault/spring-boot-max_ttl/) and the code in the `master` branch.

The blog gives the solution just for the case using `HikariDataSource` because you can change the credentials on-the-fly.

The application gives a further solution for more general cases with replacing and refreshing the `Environment`.
 
It has been tested with OpenJDK11-OpenJ9, vault v1.3.2 and MongoDB v3.6.

# Getting Started

## Vault
The application uses **APPROLE** authentication, and here is the configuration of vault as follows.
1. Start `Vault` server in development mode
    ```
    vault server -dev
    ```
2. Export an environment variable for local connection
    ```
    export VAULT_ADDR='http://127.0.0.1:8200'
    ```
3. Create a new auth method named __"approle"__
    ```
    vault auth enable approle
    ```
4. Create a new policy path named __"app-policy"__ and use hcl file to create policies of the path in order to get/renew database credentials (The fcl file locates under `vault` folder.)
    ```
    vault write sys/policy/app-policy policy=@app-policy.hcl
    ```
5. Create a `readwrite` path in "approle" with unlimited tokens and secret ids
    ```
    vault write auth/approle/role/readwrite secret_id_ttl=100m token_num_uses=0 token_ttl=100m token_max_ttl=100m secret_id_num_uses=0 policies="default,app-policy"
    ```
6. (Optional) Enable database secrets if it disabled
    ```
    vault secrets enable database
    ```
7. Create a MongoDB config named __"app-mongodb"__ and add its connection info.
    ```
    vault write database/config/app-mongodb plugin_name=mongodb-database-plugin allowed_roles="readwrite" connection_url="mongodb://{{username}}:{{password}}@[myip]/admin?ssl=false" username="[myusername]" password="[mypassword]"
    ```
8.  Configure a role that maps the MongoDB config to a MongoDB command that executes and creates the database credential (Here, `ttl` is 2 minutes and `max_ttl` is 5 minutes) 
    ```
    vault write database/roles/readwrite db_name=app-mongodb creation_statements='{ "db": "vaultdemo", "roles": [{ "role": "dbOwner" }, {"role": "readWrite", "db": "vaultdemo"}] }' revocation_statements='{"db":"vaultdemo"}' default_ttl="2m" max_ttl="5m"
    ```
9. Get `role-id` which will be the value of `spring.cloud.vault.app-role.role-id`
    ```
    vault read auth/approle/role/readwrite/role-id
    ```
10. Get `secret-id` which will be the value of `spring.cloud.vault.app-role.secret-id`
    ```
    vault write -f auth/approle/role/readwrite/secret-id
    ```
## Spring boot
1. Configure `spring.cloud.vault.app-role.role-id` and `spring.cloud.vault.app-role.secret-id` in `bootstrap.yml`.
2. Configure host and port of MongoDB in `application.yml`.
3. Start the application.
3. Access POST http://localhost:8080 to init some data and GET http://localhost:8080 to retrieve them. 

# Console Output
There are several logs shows the application do renew the database credential. 

After about 5 minutes, the lease is expired (because `max_ttl` is `5m`), and the application requests to rotate a new lease.

When the application obtain the new credential, it replaces the one values in `Environment` and then makes refresh. You can see the application has a shot time reboot in console (<500ms in my case).

Please pay attention to the `mongoClient()` method which has `@RefreshScope` annotation. It is because after the `Environment` refreshed, the `MongoClient` must be changed accordingly.

#Reference
[Dynamic Configuration Properties in Spring Boot and Spring Cloud](https://gist.github.com/dsyer/a43fe5f74427b371519af68c5c4904c7)

[How to hot-reload properties in Java EE and Spring Boot](https://stackoverflow.com/questions/52594764/how-to-hot-reload-properties-in-java-ee-and-spring-boot/52648630#52648630)
