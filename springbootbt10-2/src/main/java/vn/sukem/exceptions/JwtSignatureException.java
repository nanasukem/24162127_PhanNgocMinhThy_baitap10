package vn.sukem.exceptions;

public class JwtSignatureException extends RuntimeException {
	public JwtSignatureException(String message) {
		super(message);
	}
}