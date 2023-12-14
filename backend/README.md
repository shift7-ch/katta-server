# Cryptomator Hub Backend

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Dev Mode

You can run your application in dev mode that enables live coding using:
```shell script
mvn clean quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

To use proxyman for debgging, add the following lines to `/etc/hosts`:
```
127.0.0.1 proxyman.local
::1 proxyman.local
```
Some browsers do not forward any requests to `localhost`, see [Proxyman Documentation](https://docs.proxyman.io/troubleshooting/couldnt-see-any-request-from-localhost-server) for more information.
Caveat: `Proxyman` seems to modify some requests and invalidate JWT signatures. To be confirmed.

```shell
mvn clean quarkus:dev -Dquarkus.profile=proxyman -Dquarkus.config.profile.parent=dev
```
https://quarkus.io/guides/config-reference

### Accessing Keycloak (Port 8180)

During development, Keycloak is started as a Quarkus Dev Service using port 8180. When using alternative ports, you can also find it via [http://localhost:8080/q/dev](http://localhost:8080/q/dev).


### Testing rest services via CLI:

First, access the keycloak admin web console and activate direct access grants for the `cryptomator` realm.

Then, retrieve an `access_token` from keycloak:

```
export access_token=$(\
    curl -X POST http://localhost:8180/auth/realms/cryptomator/protocol/openid-connect/token \
    --user admin:admin \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=owner&password=owner&grant_type=password' | jq --raw-output '.access_token' \
)
```

Then use this token as a Bearer Token:

```shell
curl -v -X GET \
  http://localhost:8080/users/me \
  -H "Authorization: Bearer "$access_token
```

## Packaging

Make sure a container engine is running (required to register the built image locally).

Then run this command to build the image:

```shell script
mvn clean package -Dquarkus.container-image.build=true -Dquarkus.container-image.tag=latest
```

### Using containerd or podman

Tell JIB which executable to use (replace `nerctl` with `podman` etc):

```shell script
 -Dquarkus.jib.docker-executable-name=$(which nerdctl)
```

### Building native images

3x smaller but takes longer to build. Docker VM requires sufficient memory during the build:
```shell script
mvn clean package -Pnative -Dquarkus.container-image.build=true -Dquarkus.native.container-build=true -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:22.2-java17 -Dquarkus.container-image.tag=latest
```
