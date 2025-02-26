using Microsoft.AspNetCore.Mvc;
using PayamaX.Portal.Contracts;
using PayamaX.Portal.UseCases.UploadPayamak;

namespace PayamaX.Portal.Controllers;

[ApiController]
[Route("/payamax/manager/")]
public class PayamaxManagerController(IPayamaksContract payamaksContract) : ControllerBase
{
    [HttpPost("download")]
    public Task<IList<ExpectedPayamakProcessResult>> DownloadPayamaks(CancellationToken cancellationToken = default)
    {
        return payamaksContract.List(cancellationToken);
    }
}