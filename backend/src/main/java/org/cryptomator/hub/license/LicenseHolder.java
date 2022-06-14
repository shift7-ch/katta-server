package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.cryptomator.hub.entities.Billing;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class LicenseHolder {

	private static final Logger LOG = Logger.getLogger(LicenseHolder.class);
	private final LicenseValidator licenseValidator;
	private volatile DecodedJWT license;

	LicenseHolder(LicenseValidator licenseValidator) {
		this.licenseValidator = licenseValidator;
	}

	/**
	 * Loads the license from the database, if present
	 */
	@PostConstruct
	void init() {
		var billingEntry = Billing.<Billing>findAll().firstResult();
		if(billingEntry.token != null) {
			try {
				this.license = licenseValidator.validate(billingEntry.token, billingEntry.hubId);
			} catch (JWTVerificationException e) {
				LOG.warn("License in database is invalid. Deleting entry. Please add the license over the REST API again.");
				billingEntry.token = null;
				billingEntry.persist();
			}
		}
	}

	/**
	 * Parses, verifies and persists the given token as the license in the database.
	 *
	 * @param token The string represenation of the JWT license
	 * @throws JWTVerificationException if the license cannot be verfied
	 */
	@Transactional
	public synchronized void set(String token) throws JWTVerificationException {
		Objects.requireNonNull(token);

		var billingEntry = Billing.<Billing>findAll().firstResult();
		this.license = licenseValidator.validate(token, billingEntry.hubId);
		billingEntry.token = token;
		billingEntry.persist();
	}

	public DecodedJWT get() {
		return license;
	}

	public boolean isExpired() {
		return Optional.ofNullable(license).map(l -> l.getExpiresAt().toInstant().isBefore(Instant.now())).orElse(true);
	}

	public long getAvailableSeats() {
		return Optional.ofNullable(license) //
				.map(l -> l.getClaim("seats")) //
				.map(Claim::asLong) //
				.orElse(0L);
	}

}