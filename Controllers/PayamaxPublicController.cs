using Microsoft.AspNetCore.Mvc;
using PayamaX.Portal.Contracts;
using PayamaX.Portal.UseCases.UploadPayamak;

namespace PayamaX.Portal.Controllers;

[ApiController]
[Route("/payamax/pub/")]
public class PayamaxPublicController(IPayamaksContract payamaksContract) : ControllerBase
{
    [HttpPost("upload")]
    public Task<UploadPayamakOutput> UploadPayamak(UploadPayamakInput input,
        CancellationToken cancellationToken = default)
    {
        return payamaksContract.Upload(input, cancellationToken);
    }
}