package sg.edu.nus.iss.shoppingcart.service;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.shoppingcart.dto.OrderDetailDto;
import java.io.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** A printable receipt built entirely from immutable order/payment snapshots. */
@Service
public class PaymentReceiptPdfService {
    public byte[] create(OrderDetailDto order) {
        try(var document=new PDDocument(); var fontStream=getClass().getResourceAsStream("/fonts/NotoSansSC-Regular.ttf")) {
            if(fontStream==null) throw new IOException("Receipt font is missing");
            PDFont font=PDType0Font.load(document,fontStream,true);
            document.getDocumentInformation().setTitle("NEXUS Digital payment receipt - " + order.getId());
            document.getDocumentInformation().setAuthor("NEXUS Digital");
            try(var layout=new Layout(document,font)) {
                layout.page();
                layout.text("NEXUS Digital",48,layout.y,23);layout.y-=26;
                layout.text("PAYMENT RECEIPT",48,layout.y,13);layout.y-=32;
                layout.line("Receipt no.","NX-"+order.getId());
                layout.line("Order no.","#"+order.getId());
                var payment=order.getPayment();
                var time=payment!=null && payment.paidAt()!=null ? payment.paidAt() : order.getCreatedAt();
                layout.line("Payment date",time.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm",Locale.ENGLISH))+" (SGT)");
                layout.line("Status","PAID");
                layout.line("Currency","SGD - Singapore Dollar");
                if(payment!=null && payment.instrument()!=null)
                    layout.line("Payment method",payment.instrument()+(payment.lastDigits()==null?"":"  **** "+payment.lastDigits()));
                if(payment!=null && payment.reference()!=null) {
                    layout.text("Transaction reference",48,layout.y,9);layout.y-=17;
                    layout.paragraph(payment.reference(),9,499);layout.y-=8;
                }
                if(order.getShipping()!=null) {
                    layout.line("Customer",order.getShipping().getRecipientName());
                    layout.text("Delivery address",48,layout.y,9);layout.y-=17;
                    layout.paragraph(order.getShipping().getFullAddress(),9,499);layout.y-=10;
                }
                layout.tableHeader();
                for(var item:order.getItems()) {
                    var name=layout.wrap(item.getProductName(),10,255);
                    float height=Math.max(1,name.size())*15+17;
                    if(layout.y-height<78){layout.page();layout.tableHeader();}
                    float top=layout.y;
                    for(var line:name){layout.text(line,48,layout.y,10);layout.y-=15;}
                    layout.right(String.valueOf(item.getQuantity()),342,top,10);
                    layout.right(money(item.getUnitPrice()),444,top,10);
                    layout.right(money(item.getSubtotal()),547,top,10);
                    layout.y=top-height;
                    layout.rule(layout.y+7);
                }
                layout.ensure(170);layout.y-=12;
                var pricing=order.getPricing();
                if(pricing!=null) {
                    layout.total("Original subtotal",money(pricing.originalSubtotal()),10);
                    layout.total("Discount savings","-"+money(pricing.discountAmount()),10);
                    layout.total("Subtotal after discounts",money(pricing.subtotal()),10);
                    layout.total("GST (9%)",money(pricing.gstAmount()),10);
                }
                layout.y-=5;layout.rule(layout.y+12);
                layout.total("TOTAL PAID", "SGD "+money(order.getTotalAmount()),13);
                layout.y-=22;layout.paragraph("Thank you for shopping with NEXUS Digital.",10,499);
            }
            int pages=document.getNumberOfPages();
            for(int n=0;n<pages;n++) try(var stream=new PDPageContentStream(document,document.getPage(n),PDPageContentStream.AppendMode.APPEND,true)) {
                stream.setNonStrokingColor(0.4f,0.45f,0.5f);stream.beginText();stream.setFont(font,8);stream.newLineAtOffset(48,35);
                stream.showText("NEXUS Digital  |  Receipt NX-"+order.getId()+"  |  Page "+(n+1)+" of "+pages);stream.endText();
            }
            var output=new ByteArrayOutputStream();document.save(output);return output.toByteArray();
        } catch(IOException ex) { throw new IllegalStateException("Could not generate payment receipt",ex); }
    }
    private static String money(BigDecimal amount){return amount.setScale(2,java.math.RoundingMode.HALF_UP).toPlainString();}
    private static class Layout implements AutoCloseable {
        final PDDocument document; final PDFont font; PDPageContentStream stream; float y;
        Layout(PDDocument d,PDFont f){document=d;font=f;}
        void page() throws IOException {if(stream!=null)stream.close();var page=new PDPage(PDRectangle.A4);document.addPage(page);stream=new PDPageContentStream(document,page);y=790;}
        void ensure(float height)throws IOException{if(y-height<65)page();}
        String printable(String value)throws IOException {
            var out=new StringBuilder();
            for(int cp:value.codePoints().toArray()) {
                if(Character.isISOControl(cp)){out.append(' ');continue;}
                String glyph=new String(Character.toChars(cp));
                try{font.encode(glyph);out.append(glyph);}catch(IllegalArgumentException ex){out.append('?');}
            }
            return out.toString();
        }
        void text(String value,float x,float baseline,float size)throws IOException {
            stream.setNonStrokingColor(0.12f,0.17f,0.25f);stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(x,baseline);stream.showText(printable(value));stream.endText();
        }
        void right(String value,float x,float baseline,float size)throws IOException {String v=printable(value);text(v,x-font.getStringWidth(v)*size/1000,baseline,size);}
        void rule(float baseline)throws IOException {stream.setStrokingColor(0.85f,0.88f,0.91f);stream.setLineWidth(.5f);stream.moveTo(48,baseline);stream.lineTo(547,baseline);stream.stroke();}
        List<String> wrap(String value,float size,float width)throws IOException {
            var result=new ArrayList<String>();String current="";
            for(int cp:printable(value).codePoints().toArray()) {
                String next=current+new String(Character.toChars(cp));
                if(!current.isEmpty() && font.getStringWidth(next)*size/1000>width){result.add(current);current=new String(Character.toChars(cp));}else current=next;
            }
            if(!current.isEmpty())result.add(current);return result;
        }
        void paragraph(String value,float size,float width)throws IOException {for(String line:wrap(value,size,width)){ensure(17);text(line,48,y,size);y-=17;}}
        void line(String label,String value)throws IOException {var lines=wrap(value,10,340);ensure(Math.max(1,lines.size())*17+3);text(label,48,y,9);for(String line:lines){text(line,203,y,10);y-=17;}y-=3;}
        void total(String label,String value,float size)throws IOException {text(label,260,y,size);right(value,547,y,size);y-=23;}
        void tableHeader()throws IOException {ensure(60);y-=12;text("ITEM",48,y,9);right("QTY",342,y,9);right("UNIT (SGD)",444,y,9);right("AMOUNT (SGD)",547,y,9);y-=10;rule(y);y-=24;}
        public void close()throws IOException{if(stream!=null)stream.close();}
    }
}
