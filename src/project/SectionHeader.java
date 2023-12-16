package project;

public class SectionHeader {
    private int sh_name;
    private int sh_type;
    private int sh_flags;
    private int sh_address;
    private int sh_offset;
    private int sh_size;
    private int sh_link;
    private int sh_info;
    private int sh_address_align;
    private int sh_entry_size;

    @Override
    public String toString() {
        return "SectionHeader{" + "\n" +
                "sh_name=" + sh_name + "\n" +
                ", sh_type=" + sh_type + "\n" +
                ", sh_flags=" + sh_flags + "\n" +
                ", sh_address=" + sh_address + "\n" +
                ", sh_offset=" + sh_offset + "\n" +
                ", sh_size=" + sh_size + "\n" +
                ", sh_link=" + sh_link + "\n" +
                ", sh_info=" + sh_info + "\n" +
                ", sh_address_align=" + sh_address_align + "\n" +
                ", sh_entry_size=" + sh_entry_size + "\n" +
                '}' + "\n";
    }

    public int getSh_name() {
        return sh_name;
    }

    public int getAddress() {
        return sh_address;
    }

    public void setSh_name(int sh_name) {
        this.sh_name = sh_name;
    }

    public int getSh_type() {
        return sh_type;
    }

    public void setSh_type(int sh_type) {
        this.sh_type = sh_type;
    }

    public int getSh_flags() {
        return sh_flags;
    }

    public void setSh_flags(int sh_flags) {
        this.sh_flags = sh_flags;
    }

    public void setSh_address(int sh_address) {
        this.sh_address = sh_address;
    }

    public int getSh_offset() {
        return sh_offset;
    }

    public void setSh_offset(int sh_offset) {
        this.sh_offset = sh_offset;
    }

    public int getSh_size() {
        return sh_size;
    }

    public void setSh_size(int sh_size) {
        this.sh_size = sh_size;
    }

    public int getSh_link() {
        return sh_link;
    }

    public void setSh_link(int sh_link) {
        this.sh_link = sh_link;
    }

    public void setSh_info(int sh_info) {
        this.sh_info = sh_info;
    }

    public void setSh_address_align(int sh_address_align) {
        this.sh_address_align = sh_address_align;
    }

    public int getSh_entry_size() {
        return sh_entry_size;
    }

    public void setSh_entry_size(int sh_entry_size) {
        this.sh_entry_size = sh_entry_size;
    }
}
