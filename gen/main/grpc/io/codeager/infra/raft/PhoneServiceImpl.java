package main.grpc.io.codeager.infra.raft;

import io.codeager.infra.raft.PhoneServiceGrpc;
import io.codeager.infra.raft.Phonebook;
import io.grpc.stub.StreamObserver;

/**
 * @author Jiupeng Zhang
 * @since 04/24/2019
 */
public class PhoneServiceImpl extends PhoneServiceGrpc.PhoneServiceImplBase {
    @Override
    public void addPhoneToUser(Phonebook.AddPhoneToUserRequest request, StreamObserver<Phonebook.AddPhoneToUserResponse> responseObserver) {
        Phonebook.AddPhoneToUserResponse response = null;
        if(request.getPhoneNumber().length() == 11 ){
            System.out.printf("uid = %s , phone type is %s, nubmer is %s/n", request.getUid(), request.getPhoneType(), request.getPhoneNumber());
            response = Phonebook.AddPhoneToUserResponse.newBuilder().setResult(true).build();
        }else{
            System.out.printf("The phone nubmer %s is wrong!/n",request.getPhoneNumber());
            response = Phonebook.AddPhoneToUserResponse.newBuilder().setResult(false).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
