package br.com.zup.edu

import br.com.zup.edu.carros.Carro
import br.com.zup.edu.carros.CarroRepository
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.Assert.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false)
class CarrosGrpcTest(
    val repository: CarroRepository,
    val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub
) {

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    /**
     * 1- happy path
     * 2- Placa não pode ser existente
     * 3- Validação do hibernate
     */

    @Test
    fun `deve adicionar um novo carro`() {
        //cenário

        //ação
        val response = grpcClient.adicionar(CarroRequest.newBuilder().setModelo("Gol").setPlaca("HIX-1234").build())

        //validação
        with(response) {
            assertNotNull(id)
            assertTrue(repository.existsById(id)) // efeito colateral
        }
    }

    @Test
    fun `nao deve adicionar novo carro com placa cadastrada`() {
        // cenario
        val existente = repository.save(Carro(modelo = "Ranger", placa = "ZUP-9876"))

        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(
                CarroRequest.newBuilder()
                    .setModelo("Ranger")
                    .setPlaca(existente.placa)
                    .build()
            )
        }

        // validação
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Placa já cadastrada", status.description)
        }
    }

    @Test
    fun `nao deve adicionar novo carro com daddos invalidos`() {
        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(CarroRequest.newBuilder().build())
        }

        // validação
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }
}

@Factory
class Clients {
    @Singleton
    fun clientStubs(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub? {
        return CarrosGrpcServiceGrpc.newBlockingStub(channel)
    }
}