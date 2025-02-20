
using api.UseCases.UploadPayamak;
using Microsoft.AspNetCore.Mvc;

namespace api.Controllers;

[ApiController()]
public class PayamaxUserController : ControllerBase{
    [HttpPost("api/payamax/user/upload")]
    public UploadPayamakOutput UploadPayamak(UploadPayamakInput input)
    {
        throw new System.NotImplementedException();
    }
}