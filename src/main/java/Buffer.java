import java.util.ArrayList;
import java.util.List;

public class Buffer {
    public List<String> newFileNames = new ArrayList<>();
    public List<String> updateFileNames = new ArrayList<>();
    public List<String> fileUpdate = new ArrayList<>();
    public List<String> deleteFilenames = new ArrayList<>();
    public int clientId;

    public Buffer(int id) {
        this.clientId = id;
    }

    public String checkUpdate(){
        StringBuilder out = new StringBuilder();
        if(this.newFileNames.size()>0){
            out.append("ADD#");
            out.append(this.newFileNames.get(0)).append("#");
            out.append("STOP");
            return out.toString();
        }
        if(this.updateFileNames.size()>0){
            out.append("UPDATE#");
            out.append(this.updateFileNames.get(0)).append("#").append(this.fileUpdate.get(0)).append("#");
            out.append("STOP");
            return out.toString();
        }
        if(this.deleteFilenames.size()>0){
            out.append("DELETE#");
            out.append(this.deleteFilenames.get(0)).append("#");
            out.append("STOP");
            return out.toString();
        }
        return out.toString().equals("") ?
                "NONE":
                out.toString();
    }

    public void setNewFileNames(String newFileName) {
        this.newFileNames.add(newFileName);
    }

    public void setUpdateFileNames(String updateFileName, String update) {
        this.updateFileNames.add(updateFileName);
        this.fileUpdate.add(update);
    }

    public void setDeleteFilenames(String deleteFilename) {
        this.deleteFilenames.add(deleteFilename);
    }

    public void deleteNewFileNames(){
        this.newFileNames.remove(0);
    }

    public void deleteUpdateFileNames(){
        this.updateFileNames.remove(0);
        this.fileUpdate.remove(0);
    }

    public void deleteDeleteFilenames(){
        this.deleteFilenames.clear();
    }

}
