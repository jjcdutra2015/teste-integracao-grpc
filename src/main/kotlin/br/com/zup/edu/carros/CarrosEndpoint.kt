package br.com.zup.edu.carros

import br.com.zup.edu.CarroRequest
import br.com.zup.edu.CarroResponse
import br.com.zup.edu.CarrosGrpcServiceGrpc
import io.grpc.Status
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
class CarrosEndpoint(@Inject val repository: CarroRepository) : CarrosGrpcServiceGrpc.CarrosGrpcServiceImplBase() {

    override fun adicionar(request: CarroRequest, responseObserver: StreamObserver<CarroResponse>) {

        if (repository.existsByPlaca(request.placa)) {
            responseObserver.onError(Status.ALREADY_EXISTS
                .withDescription("Placa já cadastrada")
                .asException())
            return
        }

        val carro = Carro(
            modelo = request.modelo,
            placa = request.placa
        )

        try {
            repository.save(carro)
        } catch (e: ConstraintViolationException) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("Dados inválidos")
                .asException()
            )
            return
        }

        responseObserver.onNext(CarroResponse.newBuilder().setId(carro.id!!).build())
        responseObserver.onCompleted()
    }
}