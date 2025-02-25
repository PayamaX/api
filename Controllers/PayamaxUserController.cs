using Microsoft.AspNetCore.Mvc;
using PayamaX.Portal.Contracts;
using PayamaX.Portal.UseCases.UploadPayamak;

namespace PayamaX.Portal.Controllers;

[ApiController]
[Route("/payamax/all/")]
public class PayamaxUserController : ControllerBase
{
    private readonly PayamaksContract payamaksContract;

    public PayamaxUserController(PayamaksContract payamaksContract)
    {
        this.payamaksContract = payamaksContract;
    }

    [HttpPost("upload")]
    public Task<UploadPayamakOutput> UploadPayamak(UploadPayamakInput input,
        CancellationToken cancellationToken = default)
    {
        return payamaksContract.Upload(input, cancellationToken);
    }
}