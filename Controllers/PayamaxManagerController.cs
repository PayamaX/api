using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PayamaX.Portal.Contracts;
using PayamaX.Portal.UseCases.UploadPayamak;

namespace PayamaX.Portal.Controllers;

[ApiController]
[Route("/payamax/manager/")]
[ApiExplorerSettings(GroupName = "manager")]
[Authorize]
public class PayamaxManagerController(IPayamaksContract payamaksContract) : ControllerBase
{
    [HttpPost("download")]
    public Task<IList<PayamakExpectedProcessResultPortable>> DownloadPayamaks(CancellationToken cancellationToken = default)
    {
        return payamaksContract.List(cancellationToken);
    }
}