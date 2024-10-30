package com.tamagoseed.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import com.tamagoseed.model.Cliente;
import com.tamagoseed.model.ClienteLogin;
import com.tamagoseed.repository.ClienteRepository;
import com.tamagoseed.security.JwtService;

@Service
public class ClienteService {
    
    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    public Optional<Cliente> cadastrarCliente(Cliente cliente) {
        if (clienteRepository.findByEmail(cliente.getEmail()).isPresent() || clienteRepository.findByCnpj(cliente.getCnpj()).isPresent()) {
            return Optional.empty();
        }

        cliente.setSenha(criptografarSenha(cliente.getSenha())); 
        return Optional.of(clienteRepository.save(cliente));
    }

    private String criptografarSenha(String senha) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(senha);
    }
        
    public Optional<Cliente> atualizarCliente(Cliente cliente) {
        if (clienteRepository.findById(cliente.getId()).isPresent()) {
            Optional<Cliente> buscaUsuario = clienteRepository.findByEmail(cliente.getEmail());

            if (buscaUsuario.isPresent() && buscaUsuario.get().getId() != cliente.getId()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já existe!", null);
            }

            cliente.setSenha(criptografarSenha(cliente.getSenha()));
            return Optional.ofNullable(clienteRepository.save(cliente));
        }

        return Optional.empty();
    }
        
    public Optional<ClienteLogin> autenticarCliente(ClienteLogin clienteLogin) {
        var credenciais = new UsernamePasswordAuthenticationToken(
            clienteLogin.getEmail(),
            clienteLogin.getSenha()
        );

        Authentication authentication = authenticationManager.authenticate(credenciais);

        if (authentication.isAuthenticated()) {
            Optional<Cliente> cliente = clienteRepository.findByEmail(clienteLogin.getEmail());

            if (cliente.isPresent()) {
                clienteLogin.setId(cliente.get().getId());
                clienteLogin.setRazaoSocial(cliente.get().getRazaoSocial());
                clienteLogin.setCnpj(cliente.get().getCnpj());
                clienteLogin.setFoto(cliente.get().getFoto());
                clienteLogin.setToken(gerarToken(clienteLogin.getEmail()));
                clienteLogin.setSenha(""); // Limpa a senha
                return Optional.of(clienteLogin);
            }
        }
        return Optional.empty();
    }

    private String gerarToken(String usuario) {
        return "Bearer " + jwtService.generateToken(usuario);
    }
}
