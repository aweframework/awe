package com.almis.awe.dao;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.User;

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
