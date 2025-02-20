using api.Commons;

namespace api.UseCases.UploadPayamak;

public record ExpectedPayamakProcessResult(PayamakOrigin Origin, string Body, long ReceivedEpochMillis);