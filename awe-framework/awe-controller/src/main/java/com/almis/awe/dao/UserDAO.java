package com.almis.awe.dao;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.User;
import com.almis.awe.model.dto.UserProfile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserDAO {

	/**
	 * Find user by name. Used in authentication process.
	 *
	 * @param userName User name
	 * @return <code>UserDetails</code> with credentials. It can be
	 *         <code>null</code> value if user don´t exist.
	 */
	User findByUserName(String userName);

	/**
	 * Find user by email. Used in oauth2 authentication process.
	 *
	 * @param email Email user
	 * @return <code>UserDetails</code> with credentials. It can be
	 *         <code>null</code> value if user don´t exist.
	 */
	User findByEmail(String email);

	/**
	 * Find user info by role. Used in oauth2 authentication process.
	 *
	 * @param role Granted authority
	 * @return <code>UserDetails</code> with credentials. It can be
	 *         <code>null</code> value if user don´t exist.
	 */
	User findByRole(String role);

	/**
	 * Check role if exists. Used in oauth2 authentication process.
	 *
	 * @param role Granted authority
	 * @return <code>UserDetails</code> with credentials. It can be
	 *         <code>null</code> value if user don´t exist.
	 */
	boolean existRole(String role) throws AWException;

	/**
	 * Find the id of a profile by its name. Used to store the additional profiles of an oauth2 user.
	 *
	 * @param profile Profile name (case-insensitive)
	 * @return Profile id, empty if there is no profile with that name
	 * @throws AWException Error retrieving the profile
	 */
	default Optional<Integer> findProfileId(String profile) throws AWException {
		return Optional.empty();
	}

	/**
	 * Find a profile by its name, as {@link #findProfileId(String)} finds it (case-insensitive). Used to map the roles of
	 * an access token to AWE profiles.
	 *
	 * @param profile Profile name (case-insensitive)
	 * @return Profile with its acronym, name and restriction file, empty if there is no profile with that name
	 * @throws AWException The profile cannot be retrieved: never read as a missing profile, so a failed lookup cannot
	 *                     grant the default profile
	 */
	default Optional<UserProfile> findProfile(String profile) throws AWException {
		return Optional.empty();
	}

	/**
	 * Whether this DAO reads and stores the additional profiles of a user (AweOpeRol). Without it, single sign-on never
	 * writes nor deletes the additional profiles of a user.
	 *
	 * @return true if the additional profiles of a user are supported
	 */
	default boolean supportsAdditionalProfiles() {
		return false;
	}

	/**
	 * Find the additional profiles of a user (AweOpeRol): the profiles the user has besides the main one.
	 *
	 * @param userName User name
	 * @return Additional profiles (empty when the user has none)
	 */
	default List<UserProfile> findAdditionalProfiles(String userName) {
		return Collections.emptyList();
	}

	/**
	 * Find the permissions assigned to some profiles (AweProPer): catalog identifiers or wildcards, not expanded.
	 *
	 * @param profileAcronyms Profile acronyms
	 * @return Assigned permissions (empty when the profiles have none)
	 */
	default List<String> findPermissions(List<String> profileAcronyms) {
		return Collections.emptyList();
	}

	/**
	 * Find the restriction file of some profiles (AwePro.Res): the profile file whose permissions they get.
	 *
	 * @param profileAcronyms Profile acronyms
	 * @return Restriction file by profile acronym, null when the profile has none (a profile that is not found is not
	 *         in the map)
	 */
	default Map<String, String> findProfileRestrictions(List<String> profileAcronyms) {
		return Collections.emptyMap();
	}

	/**
	 * Find the permissions assigned to every profile (AweProPer): catalog identifiers or wildcards, not expanded.
	 *
	 * @return Assigned permissions by profile acronym (empty when no profile has any)
	 */
	default Map<String, List<String>> findAllPermissions() {
		return Collections.emptyMap();
	}

	/**
	 * Store a new password hash for a user. Used to rehash legacy passwords on a successful login.
	 *
	 * @param userName        User name
	 * @param encodedPassword New password hash
	 * @return true if the password hash has been stored
	 */
	default boolean updatePassword(String userName, String encodedPassword) {
		return false;
	}
}
