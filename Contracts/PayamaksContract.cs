using PayamaX.Portal.UseCases.UploadPayamak;

namespace PayamaX.Portal.Contracts;

public interface PayamaksContract
{
    Task<UploadPayamakOutput> Upload(UploadPayamakInput input, CancellationToken cancellationToken = default);
}