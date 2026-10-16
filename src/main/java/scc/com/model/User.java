package scc.com.model;

/**
 * MODEL layer - immutable data holder for one row of the "users" table.
 */
public record User(int id, String username, String password, String roles, String status) {
}