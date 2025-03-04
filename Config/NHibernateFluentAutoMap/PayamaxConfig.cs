using FluentNHibernate.Automapping;

namespace PayamaX.Portal.Config.NHibernateFluentAutoMap;

public class PayamaxConfig : DefaultAutomappingConfiguration
{
    public override bool ShouldMap(Type type)
    {
        return type.Name.EndsWith("Entity");
    }

    public override bool IsComponent(Type type)
    {
        return type.Name.EndsWith("Component");
    }
    
}