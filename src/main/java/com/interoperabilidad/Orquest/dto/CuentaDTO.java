package com.interoperabilidad.Orquest.dto;

public class CuentaDTO {
    private Long id;
    private Long numeroCuenta;
    private String direccion;
    private String telefono;

//    public CuentaDTO(Long id, Long numeroCuenta, String direccion, String telefono) {
//        this.id = id;
//        this.numeroCuenta = numeroCuenta;
//        this.direccion = direccion;
//        this.telefono = telefono;
//    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(Long numeroCuenta) { this.numeroCuenta = numeroCuenta; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
