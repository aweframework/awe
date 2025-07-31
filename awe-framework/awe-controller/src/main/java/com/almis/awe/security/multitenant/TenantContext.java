package com.almis.awe.security.multitenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Current tenant context using ThreadLocal storage
 */
@Component
@Slf4j
public class TenantContext {

	private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

	/**
	 * Private constructor to prevent instantiation of the class.
	 */
	private TenantContext() {
	}

	/**
	 * Sets the current tenant for the thread
	 *
	 * @param tenant The tenant identifier
	 */
	public static void setCurrentTenant(String tenant) {
		log.debug("[Thread: {}] Setting current tenant: {}", Thread.currentThread().getName(), tenant);
		currentTenant.set(tenant);
	}

	/**
	 * Gets the current tenant for the thread
	 *
	 * @return The current tenant identifier
	 */
	public static String getCurrentTenant() {
		String tenant = currentTenant.get();
		log.debug("[Thread: {}] Getting current tenant: {}", Thread.currentThread().getName(), tenant);
		return tenant;
	}

	/**
	 * Clears the current tenant from the thread
	 */
	public static void clear() {
		String tenant = currentTenant.get();
		log.debug("[Thread: {}] Clearing current tenant: {}", Thread.currentThread().getName(), tenant);
		currentTenant.remove();
	}
}