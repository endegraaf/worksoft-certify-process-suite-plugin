package org.jenkinsci.plugins.worksoftcertifyprocesssuite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class FindLayout{
    
public String getLayoutPath(int id,Connection c,String p){
int pid=id;
String path=p;
try{
Statement stm1 = c.createStatement();
while(pid!=1){
String sql = "SELECT * from [dbo].[Folder] where FolderID="+pid;
ResultSet rs = stm1.executeQuery(sql);
rs.next();
pid=rs.getInt("ParentID");
String fol=rs.getString("Name");
if(pid!=1){
path=fol+"\\"+path;
}
             }
}catch(Exception ex){
ex.printStackTrace();
}
  return path;
}

public String getRecordsetMode(int id){
switch(id){
    case 0:
        return "Read Only";
    case 1:
        return "Append";
    case 2:
        return "Clear and Append";
    case 3:
        return "Read and Update";
    default:
        return "Read Only";
}  
}

}