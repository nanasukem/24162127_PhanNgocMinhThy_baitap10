package vn.sukem.exceptions;

public class JwtExpiredException extends RuntimeException {
	public JwtExpiredException(String message) {
		super(message);
	}
}