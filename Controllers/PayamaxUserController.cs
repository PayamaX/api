
using api.UseCases.UploadPayamak;
using Microsoft.AspNetCore.Mvc;

namespace api.Controllers;

[ApiController]
[Route("/payamax/all/")]
public class PayamaxUserController : ControllerBase{
    [HttpPost("upload")]
    public UploadPayamakOutput UploadPayamak(UploadPayamakInput input)
    {
        throw new System.NotImplementedException();
    }
}