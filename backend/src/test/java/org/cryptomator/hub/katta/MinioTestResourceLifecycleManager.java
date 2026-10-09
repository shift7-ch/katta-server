package org.cryptomator.hub.katta;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * Starts a throwaway MinIO server for integration tests that need a real S3-compatible backend (see {@code StorageResourceIT}).
 *
 * <p>Replaces the quarkus-minio dev service: MinIO no longer publishes container images (docker.io/minio/minio was deleted and quay.io/minio/minio
 * refuses anonymous pulls since 2026-09, see https://github.com/shift7-ch/katta-compose/issues/22), and the community rebuild used instead runs as
 * a non-root user with a {@code VOLUME /data}. Docker creates that anonymous volume root-owned (it only copies ownership for non-empty image
 * directories; Podman chowns it, which hides the problem locally), so MinIO dies with "file access denied" on CI. The dev service offers no hook
 * for mounts, hence this resource mounts {@code /data} as a tmpfs, which both Docker and Podman create world-writable (mode 1777).
 *
 * <p>Connection details are injected into fields annotated with {@link InjectMinio} of type {@link Minio} rather than exposed as config, so test
 * classes that do not use this resource are unaffected.
 */
public class MinioTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager {

	// alpine/minio is a community rebuild of the last MinIO community release (RELEASE.2025-10-15T17-29-55Z), not an official artifact,
	// hence pinned by multi-arch index digest. Keep in sync with the chart's minio-statefulset.yaml and katta-compose.
	private static final String MINIO_IMAGE = "docker.io/alpine/minio@sha256:cf23643a6cf9ce159c57643ceb88279e431262282428c9e0bf3a7ef1a97e84b4";
	private static final int MINIO_API_PORT = 9000;
	private static final String ROOT_USER = "minioadmin";
	private static final String ROOT_PASSWORD = "minioadmin";

	private static GenericContainer<?> container;

	/**
	 * Connection details of the MinIO server started by this resource.
	 *
	 * @param endpoint  S3/STS endpoint, e.g. {@code http://localhost:32768}
	 * @param accessKey root user
	 * @param secretKey root password
	 */
	public record Minio(String endpoint, String accessKey, String secretKey) {
	}

	@Override
	public Map<String, String> start() {
		container = new GenericContainer<>(DockerImageName.parse(MINIO_IMAGE))
				.withEnv("MINIO_ROOT_USER", ROOT_USER)
				.withEnv("MINIO_ROOT_PASSWORD", ROOT_PASSWORD)
				.withTmpFs(Map.of("/data", "rw")) // Podman rejects uid=/gid= options; the default mode 1777 is enough
				.withCommand("server", "/data")
				.withExposedPorts(MINIO_API_PORT)
				.waitingFor(Wait.forHttp("/minio/health/live").forPort(MINIO_API_PORT));
		container.start();
		return Map.of();
	}

	@Override
	public void inject(TestInjector testInjector) {
		var minio = new Minio("http://%s:%d".formatted(container.getHost(), container.getMappedPort(MINIO_API_PORT)), ROOT_USER, ROOT_PASSWORD);
		testInjector.injectIntoFields(minio, new TestInjector.AnnotatedAndMatchesType(InjectMinio.class, Minio.class));
	}

	@Override
	public void stop() {
		if (container != null) {
			container.stop();
		}
	}

	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD})
	public @interface InjectMinio {
	}
}
